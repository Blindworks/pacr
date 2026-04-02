import { Component, ElementRef, ViewChild, effect, inject, signal, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FeedbackDialogService } from '../../services/feedback-dialog.service';
import { FeedbackService, FeedbackCategory } from '../../services/feedback.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-feedback-dialog',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './feedback-dialog.html',
  styleUrl: './feedback-dialog.scss'
})
export class FeedbackDialog implements OnDestroy {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  private readonly dialogService = inject(FeedbackDialogService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly translate = inject(TranslateService);

  isSubmitting = signal(false);
  submitError = signal<string | null>(null);
  submitSuccess = signal(false);

  category: FeedbackCategory = 'BUG';
  subject = '';
  message = '';

  private readonly openEffect = effect(() => {
    const open = this.dialogService.isOpen();
    if (!this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  });

  close(): void {
    this.dialogService.close();
    this.resetForm();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }

  submit(): void {
    if (!this.subject.trim() || !this.message.trim()) {
      this.submitError.set(this.translate.instant('DIALOGS.FEEDBACK_FILL_FIELDS'));
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);

    this.feedbackService.submit({
      category: this.category,
      subject: this.subject.trim(),
      message: this.message.trim()
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
        setTimeout(() => this.close(), 2000);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.submitError.set(this.translate.instant('DIALOGS.FEEDBACK_ERROR'));
      }
    });
  }

  private resetForm(): void {
    this.category = 'BUG';
    this.subject = '';
    this.message = '';
    this.isSubmitting.set(false);
    this.submitError.set(null);
    this.submitSuccess.set(false);
  }

  ngOnDestroy(): void {}
}

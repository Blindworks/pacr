import { Component, ElementRef, ViewChild, effect, inject, OnDestroy } from '@angular/core';
import { ReadinessInfoDialogService } from '../../services/readiness-info-dialog.service';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-readiness-info-dialog',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './readiness-info-dialog.html',
  styleUrl: './readiness-info-dialog.scss'
})
export class ReadinessInfoDialog implements OnDestroy {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  private readonly service = inject(ReadinessInfoDialogService);

  private readonly openEffect = effect(() => {
    const open = this.service.isOpen();
    if (!this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  });

  close(): void {
    this.service.close();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }

  ngOnDestroy(): void {}
}

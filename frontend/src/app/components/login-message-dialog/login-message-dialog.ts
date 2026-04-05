import { Component, ElementRef, ViewChild, effect, inject, signal, AfterViewInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LoginMessageService } from '../../services/login-message.service';

@Component({
  selector: 'app-login-message-dialog',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './login-message-dialog.html',
  styleUrl: './login-message-dialog.scss'
})
export class LoginMessageDialog implements AfterViewInit {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  protected readonly msgService = inject(LoginMessageService);
  private readonly viewReady = signal(false);

  ngAfterViewInit(): void {
    this.viewReady.set(true);
  }

  private readonly openEffect = effect(() => {
    const has = this.msgService.hasMessages();
    const ready = this.viewReady();
    if (!ready || !this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (has && !dialog.open) {
      dialog.showModal();
    } else if (!has && dialog.open) {
      dialog.close();
    }
  });

  dismiss(): void {
    this.msgService.dismissCurrent();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.dismiss();
  }
}

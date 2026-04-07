import { AfterViewInit, Component, ElementRef, ViewChild, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PendingFriendRequestsService } from '../../services/pending-friend-requests.service';

@Component({
  selector: 'app-pending-friend-requests-dialog',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './pending-friend-requests-dialog.html',
  styleUrl: './pending-friend-requests-dialog.scss'
})
export class PendingFriendRequestsDialog implements AfterViewInit {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  protected readonly pendingService = inject(PendingFriendRequestsService);
  private readonly router = inject(Router);
  private readonly viewReady = signal(false);

  ngAfterViewInit(): void {
    this.viewReady.set(true);
  }

  private readonly openEffect = effect(() => {
    const show = this.pendingService.shouldShow();
    const ready = this.viewReady();
    if (!ready || !this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (show && !dialog.open) {
      dialog.showModal();
    } else if (!show && dialog.open) {
      dialog.close();
    }
  });

  dismiss(): void {
    this.pendingService.dismiss();
  }

  view(): void {
    this.dismiss();
    this.router.navigate(['/community/friends'], { queryParams: { tab: 'requests' } });
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.dismiss();
  }
}

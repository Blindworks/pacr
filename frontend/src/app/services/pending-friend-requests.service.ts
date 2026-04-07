import { Injectable, computed, inject, signal } from '@angular/core';
import { FriendshipService } from './friendship.service';

const STORAGE_KEY = 'pacr-pending-friend-requests-dismissed';

@Injectable({ providedIn: 'root' })
export class PendingFriendRequestsService {
  private readonly friendshipService = inject(FriendshipService);

  readonly pendingCount = signal<number>(0);
  readonly dismissed = signal<boolean>(sessionStorage.getItem(STORAGE_KEY) === '1');

  readonly shouldShow = computed(() => this.pendingCount() > 0 && !this.dismissed());

  check(): void {
    this.friendshipService.listIncoming().subscribe({
      next: list => this.pendingCount.set(list?.length ?? 0),
      error: () => this.pendingCount.set(0)
    });
  }

  dismiss(): void {
    this.dismissed.set(true);
    sessionStorage.setItem(STORAGE_KEY, '1');
  }

  reset(): void {
    this.pendingCount.set(0);
    this.dismissed.set(false);
    sessionStorage.removeItem(STORAGE_KEY);
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { FriendshipService, Friendship, UserSearchResult, FriendActivity } from '../../services/friendship.service';

type TabKey = 'feed' | 'friends' | 'requests' | 'find';

@Component({
  selector: 'app-friends',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './friends.html',
  styleUrl: './friends.scss'
})
export class Friends implements OnInit {
  private readonly friendshipService = inject(FriendshipService);

  activeTab = signal<TabKey>('feed');
  loading = signal(false);
  error = signal<string | null>(null);

  friends = signal<Friendship[]>([]);
  incoming = signal<Friendship[]>([]);
  outgoing = signal<Friendship[]>([]);
  activity = signal<FriendActivity[]>([]);

  searchQuery = '';
  searchResults = signal<UserSearchResult[]>([]);
  searching = signal(false);
  private searchTimer: any = null;

  ngOnInit(): void {
    this.loadActivity();
  }

  setTab(tab: TabKey): void {
    this.activeTab.set(tab);
    if (tab === 'feed') this.loadActivity();
    if (tab === 'friends') this.loadFriends();
    if (tab === 'requests') this.loadRequests();
  }

  loadActivity(): void {
    this.loading.set(true);
    this.friendshipService.getActivity().subscribe({
      next: a => { this.activity.set(a); this.loading.set(false); },
      error: () => { this.error.set('Failed to load activity'); this.loading.set(false); }
    });
  }

  loadFriends(): void {
    this.loading.set(true);
    this.friendshipService.listFriends().subscribe({
      next: f => { this.friends.set(f); this.loading.set(false); },
      error: () => { this.error.set('Failed to load friends'); this.loading.set(false); }
    });
  }

  loadRequests(): void {
    this.loading.set(true);
    this.friendshipService.listIncoming().subscribe({
      next: i => { this.incoming.set(i); },
      error: () => { this.error.set('Failed to load requests'); }
    });
    this.friendshipService.listOutgoing().subscribe({
      next: o => { this.outgoing.set(o); this.loading.set(false); },
      error: () => { this.loading.set(false); }
    });
  }

  onSearchInput(): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    const q = this.searchQuery.trim();
    if (q.length < 2) {
      this.searchResults.set([]);
      return;
    }
    this.searchTimer = setTimeout(() => this.runSearch(q), 300);
  }

  runSearch(q: string): void {
    this.searching.set(true);
    this.friendshipService.search(q).subscribe({
      next: results => { this.searchResults.set(results); this.searching.set(false); },
      error: () => { this.searching.set(false); }
    });
  }

  sendRequest(user: UserSearchResult): void {
    this.friendshipService.sendRequest(user.id).subscribe({
      next: () => {
        this.searchResults.update(list => list.map(u =>
          u.id === user.id ? { ...u, friendshipStatus: 'PENDING_OUT' } : u
        ));
      },
      error: err => alert(err?.error || 'Failed to send request')
    });
  }

  accept(f: Friendship): void {
    this.friendshipService.accept(f.id).subscribe({
      next: () => {
        this.incoming.update(list => list.filter(x => x.id !== f.id));
        this.loadFriends();
      }
    });
  }

  decline(f: Friendship): void {
    this.friendshipService.decline(f.id).subscribe({
      next: () => this.incoming.update(list => list.filter(x => x.id !== f.id))
    });
  }

  cancelOutgoing(f: Friendship): void {
    this.friendshipService.remove(f.id).subscribe({
      next: () => this.outgoing.update(list => list.filter(x => x.id !== f.id))
    });
  }

  removeFriend(f: Friendship): void {
    if (!confirm('Remove friend?')) return;
    this.friendshipService.remove(f.id).subscribe({
      next: () => this.friends.update(list => list.filter(x => x.id !== f.id))
    });
  }

  formatDuration(seconds: number | null): string {
    if (!seconds) return '';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  }
}

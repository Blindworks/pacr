import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FriendshipService, Friendship, UserSearchResult, FriendActivity } from '../../services/friendship.service';
import { UserService } from '../../services/user.service';

type TabKey = 'feed' | 'friends' | 'requests' | 'find';

@Component({
  selector: 'app-friends',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DatePipe],
  templateUrl: './friends.html',
  styleUrl: './friends.scss'
})
export class Friends implements OnInit, OnDestroy {
  private readonly friendshipService = inject(FriendshipService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);

  avatarUrls = signal<Record<number, string>>({});
  private readonly avatarLoading = new Set<number>();

  avatarUrl(userId: number): string | null {
    return this.avatarUrls()[userId] ?? null;
  }

  private loadAvatar(userId: number, _filename?: string | null): void {
    if (this.avatarUrls()[userId] || this.avatarLoading.has(userId)) return;
    this.avatarLoading.add(userId);
    this.userService.getProfileImage(userId).subscribe({
      next: blob => {
        if (!blob) {
          this.avatarLoading.delete(userId);
          return;
        }
        const url = URL.createObjectURL(blob);
        this.avatarUrls.update(map => ({ ...map, [userId]: url }));
        this.avatarLoading.delete(userId);
      },
      error: () => { this.avatarLoading.delete(userId); }
    });
  }

  ngOnDestroy(): void {
    Object.values(this.avatarUrls()).forEach(url => URL.revokeObjectURL(url));
  }

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

  searchMode = signal<'name' | 'nearby'>('name');
  nearbyRadius = signal<number>(25);
  nearbyRadiusOptions = [10, 25, 50, 100];
  nearbyError = signal<string | null>(null);

  ngOnInit(): void {
    const tabParam = this.route.snapshot.queryParamMap.get('tab') as TabKey | null;
    if (tabParam && ['feed', 'friends', 'requests', 'find'].includes(tabParam)) {
      this.setTab(tabParam);
    } else {
      this.loadActivity();
    }
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
      next: a => {
        this.activity.set(a);
        a.forEach(x => this.loadAvatar(x.friendId, x.profileImageFilename));
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load activity'); this.loading.set(false); }
    });
  }

  loadFriends(): void {
    this.loading.set(true);
    this.friendshipService.listFriends().subscribe({
      next: f => {
        this.friends.set(f);
        f.forEach(x => this.loadAvatar(x.otherUser.id, x.otherUser.profileImageFilename));
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load friends'); this.loading.set(false); }
    });
  }

  loadRequests(): void {
    this.loading.set(true);
    this.friendshipService.listIncoming().subscribe({
      next: i => {
        this.incoming.set(i);
        i.forEach(x => this.loadAvatar(x.otherUser.id, x.otherUser.profileImageFilename));
      },
      error: () => { this.error.set('Failed to load requests'); }
    });
    this.friendshipService.listOutgoing().subscribe({
      next: o => {
        this.outgoing.set(o);
        o.forEach(x => this.loadAvatar(x.otherUser.id, x.otherUser.profileImageFilename));
        this.loading.set(false);
      },
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
      next: results => {
        this.searchResults.set(results);
        results.forEach(u => this.loadAvatar(u.id, u.profileImageFilename));
        this.searching.set(false);
      },
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

  setSearchMode(mode: 'name' | 'nearby'): void {
    this.searchMode.set(mode);
    this.searchResults.set([]);
    this.nearbyError.set(null);
    if (mode === 'nearby') {
      this.runNearbySearch();
    }
  }

  onNearbyRadiusChange(radius: number): void {
    this.nearbyRadius.set(Number(radius));
    if (this.searchMode() === 'nearby') this.runNearbySearch();
  }

  runNearbySearch(): void {
    const me = this.userService.currentUser();
    const radius = this.nearbyRadius();
    this.nearbyError.set(null);
    if (me?.latitude != null && me?.longitude != null) {
      this.searchNearbyAt(me.latitude, me.longitude, radius);
      return;
    }
    if (!navigator.geolocation) {
      this.nearbyError.set('GEOLOCATION_UNSUPPORTED');
      return;
    }
    this.searching.set(true);
    navigator.geolocation.getCurrentPosition(
      pos => this.searchNearbyAt(pos.coords.latitude, pos.coords.longitude, radius),
      () => {
        this.searching.set(false);
        this.nearbyError.set('LOCATION_PERMISSION_DENIED');
      }
    );
  }

  private searchNearbyAt(lat: number, lon: number, radiusKm: number): void {
    this.searching.set(true);
    this.friendshipService.searchNearby(lat, lon, radiusKm).subscribe({
      next: results => {
        this.searchResults.set(results);
        results.forEach(u => this.loadAvatar(u.id, u.profileImageFilename));
        this.searching.set(false);
      },
      error: () => { this.searching.set(false); }
    });
  }

  formatDuration(seconds: number | null): string {
    if (!seconds) return '';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  }

  formatPace(secondsPerKm: number | null): string {
    if (!secondsPerKm) return '';
    const m = Math.floor(secondsPerKm / 60);
    const s = secondsPerKm % 60;
    return `${m}'${s.toString().padStart(2, '0')}" /km`;
  }

  getSportIcon(sport: string | null): string {
    const s = (sport ?? '').toLowerCase();
    if (s.includes('run')) return 'directions_run';
    if (s.includes('cycl') || s.includes('bik') || s.includes('ride')) return 'directions_bike';
    if (s.includes('swim')) return 'pool';
    if (s.includes('walk') || s.includes('hik')) return 'hiking';
    return 'fitness_center';
  }

  getSportLabel(sport: string | null): string {
    const s = (sport ?? '').toLowerCase();
    if (s.includes('run')) return 'RUNNING';
    if (s.includes('cycl') || s.includes('bik') || s.includes('ride')) return 'CYCLING';
    if (s.includes('swim')) return 'SWIMMING';
    if (s.includes('walk') || s.includes('hik')) return 'HIKING';
    return 'TRAINING';
  }

  timeAgo(dateStr: any, startTime: any): string {
    const d = this.toDate(dateStr);
    // Only apply startTime if dateStr didn't already include time info
    if (startTime && typeof dateStr === 'string' && !dateStr.includes('T')) {
      if (Array.isArray(startTime)) {
        d.setHours(startTime[0] ?? 0, startTime[1] ?? 0, startTime[2] ?? 0);
      } else if (typeof startTime === 'string') {
        const tp = startTime.split(':').map(Number);
        d.setHours(tp[0] ?? 0, tp[1] ?? 0, tp[2] ?? 0);
      }
    }
    const diffMin = Math.floor((Date.now() - d.getTime()) / 60000);
    if (diffMin < 1) return 'JUST NOW';
    if (diffMin < 60) return `${diffMin}MIN AGO`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `${diffH}H AGO`;
    const diffD = Math.floor(diffH / 24);
    return `${diffD}D AGO`;
  }

  formatDistanceValue(km: number | null): string {
    return km != null ? km.toFixed(1) : '-';
  }

  formatDurationShort(seconds: number | null): string {
    if (!seconds) return '-';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    const mm = m.toString().padStart(2, '0');
    const ss = s.toString().padStart(2, '0');
    return h > 0 ? `${h}:${mm}:${ss}` : `${m}:${ss}`;
  }

  formatPaceShort(secondsPerKm: number | null): string {
    if (!secondsPerKm) return '-';
    const m = Math.floor(secondsPerKm / 60);
    const s = secondsPerKm % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  getMapTileUrl(lat: number | null, lng: number | null): string | null {
    if (lat == null || lng == null) return null;
    const zoom = 13;
    const x = Math.floor((lng + 180) / 360 * Math.pow(2, zoom));
    const y = Math.floor((1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2 * Math.pow(2, zoom));
    return `https://basemaps.cartocdn.com/dark_all/${zoom}/${x}/${y}@2x.png`;
  }

  toDate(value: any): Date {
    if (value instanceof Date) return value;
    if (Array.isArray(value)) return new Date(value[0], (value[1] ?? 1) - 1, value[2] ?? 1);
    if (typeof value === 'string') {
      // Try native parse first (handles ISO strings like "2026-04-06T12:00:00Z")
      const native = new Date(value);
      if (!isNaN(native.getTime())) return native;
      // Fallback: parse "YYYY-MM-DD"
      const p = value.split('-').map(Number);
      return new Date(p[0], (p[1] ?? 1) - 1, p[2] ?? 1);
    }
    return new Date();
  }

  formatTime(startTime: any): string {
    if (!startTime) return '';
    if (Array.isArray(startTime)) {
      const h = String(startTime[0] ?? 0).padStart(2, '0');
      const m = String(startTime[1] ?? 0).padStart(2, '0');
      return `${h}:${m}`;
    }
    if (typeof startTime === 'string') return startTime.substring(0, 5);
    return '';
  }
}

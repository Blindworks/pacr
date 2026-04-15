import { Component, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PublicNewsService, PublicNews, TrendingTopic } from '../../services/public-news.service';
import { FriendshipService, FriendActivity, LiveTrainingFriend } from '../../services/friendship.service';
import { ActivitySocialService, ActivityKudos, ActivityComment } from '../../services/activity-social.service';
import { UserService } from '../../services/user.service';
import { RouteMiniMapComponent } from '../shared/route-mini-map/route-mini-map';

interface FeedEntry {
  type: 'news' | 'social';
  sortTime: number;
  news?: PublicNews;
  social?: FriendActivity;
}

@Component({
  selector: 'app-news-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DatePipe, DecimalPipe, RouteMiniMapComponent],
  templateUrl: './news-hub.html',
  styleUrl: './news-hub.scss'
})
export class NewsHub implements OnInit, OnDestroy {
  private readonly newsService = inject(PublicNewsService);
  private readonly friendshipService = inject(FriendshipService);
  private readonly socialService = inject(ActivitySocialService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  loading = signal(true);
  error = signal<string | null>(null);

  featured = signal<PublicNews | null>(null);
  newsList = signal<PublicNews[]>([]);
  activities = signal<FriendActivity[]>([]);
  liveTraining = signal<LiveTrainingFriend[]>([]);
  trending = signal<TrendingTopic[]>([]);

  kudosState = signal<Record<number, ActivityKudos>>({});
  commentsCount = signal<Record<number, number>>({});

  avatarUrls = signal<Record<number, string>>({});
  private readonly avatarLoading = new Set<number>();

  activeTab = signal<'all' | 'social' | 'news'>('all');

  // Comments dialog state
  commentsOpen = signal(false);
  commentsActivityId = signal<number | null>(null);
  commentsActivityTitle = signal<string>('');
  commentsList = signal<ActivityComment[]>([]);
  commentsLoading = signal(false);
  newCommentText = '';

  currentUserId = computed(() => this.userService.currentUser()?.id ?? null);

  feed = computed<FeedEntry[]>(() => {
    const entries: FeedEntry[] = [];
    const featuredId = this.featured()?.id;
    const tab = this.activeTab();
    if (tab === 'all' || tab === 'news') {
      for (const n of this.newsList()) {
        if (featuredId != null && n.id === featuredId) continue;
        const t = n.publishedAt ? new Date(n.publishedAt).getTime() : new Date(n.createdAt).getTime();
        entries.push({ type: 'news', sortTime: t, news: n });
      }
    }
    if (tab === 'all' || tab === 'social') {
      for (const a of this.activities()) {
        const base = a.date ? new Date(a.date).getTime() : 0;
        entries.push({ type: 'social', sortTime: base, social: a });
      }
    }
    return entries.sort((x, y) => y.sortTime - x.sortTime);
  });

  ngOnInit(): void {
    this.loadAll();
  }

  ngOnDestroy(): void {
    Object.values(this.avatarUrls()).forEach(url => URL.revokeObjectURL(url));
  }

  setTab(tab: 'all' | 'social' | 'news'): void {
    this.activeTab.set(tab);
  }

  private loadAll(): void {
    this.loading.set(true);
    this.newsService.listPublished().subscribe({
      next: list => {
        this.newsList.set(list);
        const f = list.find(n => n.isFeatured) ?? null;
        this.featured.set(f);
        if (f) this.newsService.recordView(f.id).subscribe({ error: () => {} });
      },
      error: () => this.error.set('news')
    });

    this.friendshipService.getActivity().subscribe({
      next: acts => {
        this.activities.set(acts);
        acts.forEach(a => {
          this.loadAvatar(a.friendId);
          this.loadKudos(a.activityId);
          this.loadCommentCount(a.activityId);
        });
        this.loading.set(false);
      },
      error: () => { this.error.set('activity'); this.loading.set(false); }
    });

    this.friendshipService.getLiveTraining().subscribe({
      next: live => {
        this.liveTraining.set(live);
        live.forEach(l => this.loadAvatar(l.friendId));
      },
      error: () => {}
    });

    this.newsService.getTrending().subscribe({
      next: t => this.trending.set(t),
      error: () => {}
    });
  }

  private loadKudos(activityId: number): void {
    this.socialService.getKudos(activityId).subscribe({
      next: k => this.kudosState.update(m => ({ ...m, [activityId]: k })),
      error: () => {}
    });
  }

  private loadCommentCount(activityId: number): void {
    this.socialService.listComments(activityId).subscribe({
      next: list => this.commentsCount.update(m => ({ ...m, [activityId]: list.length })),
      error: () => {}
    });
  }

  private loadAvatar(userId: number): void {
    if (this.avatarUrls()[userId] || this.avatarLoading.has(userId)) return;
    this.avatarLoading.add(userId);
    this.userService.getProfileImage(userId).subscribe({
      next: blob => {
        if (!blob) { this.avatarLoading.delete(userId); return; }
        const url = URL.createObjectURL(blob);
        this.avatarUrls.update(m => ({ ...m, [userId]: url }));
        this.avatarLoading.delete(userId);
      },
      error: () => this.avatarLoading.delete(userId)
    });
  }

  avatarUrl(userId: number): string | null {
    return this.avatarUrls()[userId] ?? null;
  }

  kudosCount(activityId: number): number {
    return this.kudosState()[activityId]?.count ?? 0;
  }

  hasKudos(activityId: number): boolean {
    return this.kudosState()[activityId]?.hasKudos ?? false;
  }

  commentCount(activityId: number): number {
    return this.commentsCount()[activityId] ?? 0;
  }

  toggleKudos(activityId: number): void {
    this.socialService.toggleKudos(activityId).subscribe({
      next: k => this.kudosState.update(m => ({ ...m, [activityId]: k })),
      error: () => {}
    });
  }

  openComments(activity: FriendActivity): void {
    this.commentsActivityId.set(activity.activityId);
    this.commentsActivityTitle.set(activity.title || '');
    this.commentsList.set([]);
    this.newCommentText = '';
    this.commentsOpen.set(true);
    this.refreshComments();
  }

  closeComments(): void {
    this.commentsOpen.set(false);
    const id = this.commentsActivityId();
    if (id != null) this.loadCommentCount(id);
    this.commentsActivityId.set(null);
  }

  refreshComments(): void {
    const id = this.commentsActivityId();
    if (id == null) return;
    this.commentsLoading.set(true);
    this.socialService.listComments(id).subscribe({
      next: list => {
        this.commentsList.set(list);
        list.forEach(c => { if (c.userId != null) this.loadAvatar(c.userId); });
        this.commentsLoading.set(false);
      },
      error: () => this.commentsLoading.set(false)
    });
  }

  postComment(): void {
    const id = this.commentsActivityId();
    const text = this.newCommentText.trim();
    if (id == null || !text) return;
    this.socialService.addComment(id, text).subscribe({
      next: () => {
        this.newCommentText = '';
        this.refreshComments();
      },
      error: () => {}
    });
  }

  deleteComment(commentId: number): void {
    const id = this.commentsActivityId();
    if (id == null) return;
    this.socialService.deleteComment(id, commentId).subscribe({
      next: () => this.refreshComments(),
      error: () => {}
    });
  }

  openNews(news: PublicNews): void {
    this.newsService.recordView(news.id).subscribe({ error: () => {} });
  }

  openTopic(topic: TrendingTopic): void {
    this.router.navigate(['/news-hub'], { queryParams: { tag: topic.tag } });
  }

  formatPace(secondsPerKm: number | null): string {
    if (secondsPerKm == null) return '--';
    const m = Math.floor(secondsPerKm / 60);
    const s = Math.round(secondsPerKm % 60);
    return `${m}:${s.toString().padStart(2, '0')}/km`;
  }

  formatDuration(seconds: number | null): string {
    if (seconds == null) return '--';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  }

  /**
   * Backend sends LocalTime as "HH:mm:ss" or "HH:mm:ss.nnn…".
   * Display as "HH:mm".
   */
  formatTime(value: string | null): string {
    if (!value) return '';
    const parts = value.split(':');
    if (parts.length < 2) return value;
    return `${parts[0]}:${parts[1]}`;
  }

  /**
   * True when the activity ships a downsampled route polyline that we can render
   * via `<app-route-mini-map>`. Anything shorter than 2 points is not drawable.
   */
  hasTrack(activity: FriendActivity): boolean {
    const t = activity.previewTrack;
    return Array.isArray(t) && t.length >= 2;
  }

  /**
   * Returns a CartoDB dark basemap tile URL centered on the given coordinates.
   * Same helper as the Friends feed so social cards feel consistent.
   */
  getMapTileUrl(lat: number | null, lng: number | null): string | null {
    if (lat == null || lng == null) return null;
    const zoom = 13;
    const x = Math.floor((lng + 180) / 360 * Math.pow(2, zoom));
    const y = Math.floor(
      (1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2
      * Math.pow(2, zoom)
    );
    return `https://basemaps.cartocdn.com/dark_all/${zoom}/${x}/${y}@2x.png`;
  }
}

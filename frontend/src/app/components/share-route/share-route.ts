import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CommunityRouteService } from '../../services/community-route.service';
import { ActivityService, CompletedTraining } from '../../services/activity.service';

@Component({
  selector: 'app-share-route',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './share-route.html',
  styleUrl: './share-route.scss'
})
export class ShareRoute implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly routeService = inject(CommunityRouteService);
  private readonly activityService = inject(ActivityService);
  private readonly translate = inject(TranslateService);

  activity = signal<CompletedTraining | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);

  routeName = signal('');
  visibility = signal<'PUBLIC' | 'FOLLOWERS_ONLY'>('PUBLIC');

  ngOnInit(): void {
    const id = Number(this.activatedRoute.snapshot.paramMap.get('activityId'));
    if (!id) { this.router.navigate(['/activities']); return; }

    this.activityService.getById(id).subscribe({
      next: activity => {
        this.activity.set(activity);
        this.routeName.set(activity.activityName || 'My Route');
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('COMMUNITY.ACTIVITY_NOT_FOUND'));
        this.loading.set(false);
      }
    });
  }

  share(): void {
    const act = this.activity();
    if (!act || !this.routeName()) return;

    this.saving.set(true);
    this.error.set(null);

    this.routeService.shareRoute({
      activityId: act.id,
      name: this.routeName(),
      visibility: this.visibility()
    }).subscribe({
      next: route => {
        this.saving.set(false);
        this.router.navigate(['/community-routes', route.id]);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(typeof err.error === 'string' ? err.error : this.translate.instant('COMMUNITY.SHARE_ERROR'));
      }
    });
  }

  formatDistance(km: number | null): string {
    if (!km) return '—';
    return km < 1 ? `${Math.round(km * 1000)}m` : `${km.toFixed(1)} km`;
  }

  goBack(): void {
    this.router.navigate(['/activities']);
  }
}

import { Component, OnInit, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import {
  CommunityRouteService,
  CommunityRouteDetailDto,
  LeaderboardEntryDto,
  RouteAttemptDto
} from '../../services/community-route.service';
import { GpsStreamDto } from '../../services/activity.service';
import { ActivityMapComponent } from '../activity-map/activity-map';
import { MapDialogComponent } from '../map-dialog/map-dialog';

@Component({
  selector: 'app-community-route-detail',
  standalone: true,
  imports: [CommonModule, TranslateModule, ActivityMapComponent, MapDialogComponent],
  templateUrl: './community-route-detail.html',
  styleUrl: './community-route-detail.scss'
})
export class CommunityRouteDetail implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly routeService = inject(CommunityRouteService);

  @ViewChild('mapDialog') private mapDialog!: MapDialogComponent;

  route = signal<CommunityRouteDetailDto | null>(null);
  leaderboard = signal<LeaderboardEntryDto[]>([]);
  pendingAttempt = signal<RouteAttemptDto | null>(null);
  loading = signal(true);
  period = signal<string>('ALL_TIME');
  selectingRoute = signal(false);
  gpsData = signal<GpsStreamDto | null>(null);

  ngOnInit(): void {
    const id = Number(this.activatedRoute.snapshot.paramMap.get('id'));
    if (!id) { this.router.navigate(['/community-routes']); return; }

    this.routeService.getRouteDetail(id).subscribe({
      next: route => {
        this.route.set(route);
        this.loading.set(false);
        this.loadLeaderboard(id);
        this.loadPendingAttempt();
        this.buildGpsData(route);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/community-routes']);
      }
    });
  }

  private buildGpsData(route: CommunityRouteDetailDto): void {
    if (!route.gpsTrack || route.gpsTrack.length === 0) {
      this.gpsData.set(null);
      return;
    }
    const len = route.gpsTrack.length;
    this.gpsData.set({
      completedTrainingId: 0,
      latlng: route.gpsTrack,
      distance: new Array(len).fill(0),
      heartRate: null,
      paceSecondsPerKm: null,
      altitude: null,
      hasHeartRate: false,
      hasPace: false,
      hasAltitude: false,
    });
  }

  openMapDialog(): void {
    const gps = this.gpsData();
    if (gps) {
      this.mapDialog.open(gps, 'pace');
    }
  }

  private loadLeaderboard(routeId: number): void {
    this.routeService.getLeaderboard(routeId, this.period()).subscribe({
      next: entries => this.leaderboard.set(entries),
      error: () => {}
    });
  }

  private loadPendingAttempt(): void {
    this.routeService.getPendingAttempt().subscribe({
      next: attempt => this.pendingAttempt.set(attempt),
      error: () => {}
    });
  }

  onPeriodChange(period: string): void {
    this.period.set(period);
    const r = this.route();
    if (r) this.loadLeaderboard(r.id);
  }

  selectRoute(): void {
    const r = this.route();
    if (!r) return;
    this.selectingRoute.set(true);
    this.routeService.selectRoute(r.id).subscribe({
      next: attempt => {
        this.pendingAttempt.set(attempt);
        this.selectingRoute.set(false);
      },
      error: () => this.selectingRoute.set(false)
    });
  }

  cancelAttempt(): void {
    this.routeService.cancelPendingAttempt().subscribe({
      next: () => this.pendingAttempt.set(null),
      error: () => {}
    });
  }

  formatDistance(km: number): string {
    return km < 1 ? `${Math.round(km * 1000)}m` : `${km.toFixed(1)} km`;
  }

  formatTime(seconds: number | null): string {
    if (!seconds) return '—';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  formatPace(secPerKm: number | null): string {
    if (!secPerKm) return '—';
    const m = Math.floor(secPerKm / 60);
    const s = secPerKm % 60;
    return `${m}:${String(s).padStart(2, '0')} /km`;
  }

  goBack(): void {
    this.router.navigate(['/community-routes']);
  }
}

import { Component, OnInit, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CommunityRouteService,
  CommunityRouteDetailDto,
  LeaderboardEntryDto,
  RouteAttemptDto
} from '../../services/community-route.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-community-route-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './community-route-detail.html',
  styleUrl: './community-route-detail.scss'
})
export class CommunityRouteDetail implements OnInit, OnDestroy {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly routeService = inject(CommunityRouteService);

  route = signal<CommunityRouteDetailDto | null>(null);
  leaderboard = signal<LeaderboardEntryDto[]>([]);
  pendingAttempt = signal<RouteAttemptDto | null>(null);
  loading = signal(true);
  period = signal<string>('ALL_TIME');
  selectingRoute = signal(false);
  private map: L.Map | null = null;

  ngOnInit(): void {
    const id = Number(this.activatedRoute.snapshot.paramMap.get('id'));
    if (!id) { this.router.navigate(['/community-routes']); return; }

    this.routeService.getRouteDetail(id).subscribe({
      next: route => {
        this.route.set(route);
        this.loading.set(false);
        this.loadLeaderboard(id);
        this.loadPendingAttempt();
        setTimeout(() => this.initMap(route), 0);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/community-routes']);
      }
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
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

  private initMap(route: CommunityRouteDetailDto): void {
    if (!route.gpsTrack || route.gpsTrack.length === 0) return;

    const container = document.getElementById('route-map');
    if (!container) return;

    this.map = L.map('route-map');
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(this.map);

    const latlngs = route.gpsTrack.map(p => L.latLng(p[0], p[1]));
    const polyline = L.polyline(latlngs, { color: '#3b82f6', weight: 3 }).addTo(this.map);
    this.map.fitBounds(polyline.getBounds(), { padding: [20, 20] });
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

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CommunityRouteService, CommunityRouteDto } from '../../services/community-route.service';

@Component({
  selector: 'app-my-routes',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-routes.html',
  styleUrl: './my-routes.scss'
})
export class MyRoutes implements OnInit {
  private readonly routeService = inject(CommunityRouteService);

  routes = signal<CommunityRouteDto[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.routeService.getMyRoutes().subscribe({
      next: routes => {
        this.routes.set(routes);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  unshare(route: CommunityRouteDto): void {
    this.routeService.unshareRoute(route.id).subscribe({
      next: () => this.routes.update(list => list.filter(r => r.id !== route.id)),
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
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CommunityRouteService, CommunityRouteDto } from '../../services/community-route.service';
import { UserService } from '../../services/user.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';
import { RouteMiniMapComponent } from '../shared/route-mini-map/route-mini-map';

@Component({
  selector: 'app-community-routes',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule, ProOverlay, RouteMiniMapComponent],
  templateUrl: './community-routes.html',
  styleUrl: './community-routes.scss'
})
export class CommunityRoutes implements OnInit {
  private readonly routeService = inject(CommunityRouteService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  readonly translate = inject(TranslateService);

  routes = signal<CommunityRouteDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  radiusKm = signal(10);
  sortBy = signal<'distance' | 'popularity'>('distance');
  userLat = signal<number | null>(null);
  userLon = signal<number | null>(null);

  ngOnInit(): void {
    const user = this.userService.currentUser();
    if (!user?.communityRoutesEnabled) {
      this.router.navigate(['/settings']);
      return;
    }
    this.getUserLocation();
  }

  private getUserLocation(): void {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        pos => {
          this.userLat.set(pos.coords.latitude);
          this.userLon.set(pos.coords.longitude);
          this.loadRoutes();
        },
        () => {
          this.userLat.set(48.137154);
          this.userLon.set(11.576124);
          this.loadRoutes();
        }
      );
    } else {
      this.userLat.set(48.137154);
      this.userLon.set(11.576124);
      this.loadRoutes();
    }
  }

  loadRoutes(): void {
    const lat = this.userLat();
    const lon = this.userLon();
    if (lat === null || lon === null) return;

    this.loading.set(true);
    this.error.set(null);
    this.routeService.getNearbyRoutes(lat, lon, this.radiusKm(), this.sortBy()).subscribe({
      next: routes => {
        this.routes.set(routes);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('COMMUNITY.LOAD_ERROR'));
        this.loading.set(false);
      }
    });
  }

  onRadiusChange(value: number): void {
    this.radiusKm.set(value);
    this.loadRoutes();
  }

  onSortChange(sort: 'distance' | 'popularity'): void {
    this.sortBy.set(sort);
    this.loadRoutes();
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

  generateTags(route: CommunityRouteDto): { label: string; primary: boolean }[] {
    const tags: { label: string; primary: boolean }[] = [];
    const km = route.distanceKm;
    const elev = route.elevationGainM ?? 0;

    if (elev > 300) {
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_TECHNICAL'), primary: true });
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_TRAIL'), primary: false });
    } else if (elev > 100) {
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_HILLY'), primary: true });
    } else {
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_FLAT'), primary: false });
    }

    if (km >= 21) {
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_MARATHON_PLUS'), primary: false });
    } else if (km >= 10) {
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_LONG'), primary: false });
    } else if (km < 5) {
      tags.push({ label: this.translate.instant('COMMUNITY.TAG_FAST'), primary: false });
    }

    return tags;
  }
}

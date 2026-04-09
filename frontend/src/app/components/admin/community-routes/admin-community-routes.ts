import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import * as L from 'leaflet';
import { CommunityRouteDetailDto, CommunityRouteDto, CommunityRouteService } from '../../../services/community-route.service';
import { ThemeService } from '../../../services/theme.service';

@Component({
  selector: 'app-admin-community-routes',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DatePipe, DecimalPipe],
  templateUrl: './admin-community-routes.html',
  styleUrl: './admin-community-routes.scss'
})
export class AdminCommunityRoutes implements OnInit, OnDestroy {
  private routeService = inject(CommunityRouteService);
  private themeService = inject(ThemeService);

  routes = signal<CommunityRouteDto[]>([]);
  isLoading = signal(false);
  isUploading = signal(false);
  uploadName = '';
  selectedFile: File | null = null;
  errorMessage = signal<string | null>(null);

  // Inline rename state
  editingRouteId = signal<number | null>(null);
  editingName = signal<string>('');
  isSavingName = signal(false);
  renameError = signal<string | null>(null);

  // Map dialog
  mapRoute = signal<CommunityRouteDetailDto | null>(null);
  isLoadingMap = signal(false);
  private map: L.Map | null = null;
  readonly mapContainerId = 'admin-route-map-' + Math.random().toString(36).slice(2, 8);

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  load(): void {
    this.isLoading.set(true);
    this.routeService.adminGetAllRoutes().subscribe({
      next: data => { this.routes.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
    if (this.selectedFile && !this.uploadName) {
      this.uploadName = this.selectedFile.name.replace(/\.gpx$/i, '');
    }
  }

  upload(): void {
    if (!this.selectedFile || !this.uploadName.trim()) {
      return;
    }
    this.errorMessage.set(null);
    this.isUploading.set(true);
    this.routeService.adminUploadGpx(this.selectedFile, this.uploadName.trim()).subscribe({
      next: () => {
        this.isUploading.set(false);
        this.uploadName = '';
        this.selectedFile = null;
        const fileInput = document.getElementById('gpxFileInput') as HTMLInputElement | null;
        if (fileInput) fileInput.value = '';
        this.load();
      },
      error: (err) => {
        this.isUploading.set(false);
        this.errorMessage.set(typeof err?.error === 'string' ? err.error : 'Upload failed');
      }
    });
  }

  startEdit(route: CommunityRouteDto): void {
    this.renameError.set(null);
    this.editingRouteId.set(route.id);
    this.editingName.set(route.name);
    setTimeout(() => {
      const input = document.getElementById('edit-route-' + route.id) as HTMLInputElement | null;
      input?.focus();
      input?.select();
    }, 0);
  }

  cancelEdit(): void {
    this.editingRouteId.set(null);
    this.editingName.set('');
    this.renameError.set(null);
  }

  onEditNameChange(value: string): void {
    this.editingName.set(value);
  }

  saveEdit(route: CommunityRouteDto): void {
    const trimmed = this.editingName().trim();
    if (!trimmed) {
      this.renameError.set('Name darf nicht leer sein');
      return;
    }
    if (trimmed === route.name) {
      this.cancelEdit();
      return;
    }
    this.renameError.set(null);
    this.isSavingName.set(true);
    this.routeService.adminRenameRoute(route.id, trimmed).subscribe({
      next: (updated) => {
        this.isSavingName.set(false);
        this.routes.update(list => list.map(r => r.id === route.id ? { ...r, name: updated.name } : r));
        this.cancelEdit();
      },
      error: (err) => {
        this.isSavingName.set(false);
        this.renameError.set(typeof err?.error === 'string' ? err.error : 'Rename failed');
      }
    });
  }

  delete(route: CommunityRouteDto): void {
    if (!confirm(`Route "${route.name}" wirklich löschen?`)) return;
    this.routeService.adminDeleteRoute(route.id).subscribe({
      next: () => this.load()
    });
  }

  openMap(route: CommunityRouteDto): void {
    this.isLoadingMap.set(true);
    this.mapRoute.set(null);
    this.routeService.getRouteDetail(route.id).subscribe({
      next: (detail) => {
        this.mapRoute.set(detail);
        this.isLoadingMap.set(false);
        setTimeout(() => this.initMap(detail), 50);
      },
      error: () => this.isLoadingMap.set(false)
    });
  }

  closeMap(): void {
    this.destroyMap();
    this.mapRoute.set(null);
  }

  private initMap(detail: CommunityRouteDetailDto): void {
    const container = document.getElementById(this.mapContainerId);
    if (!container) return;
    this.destroyMap();

    const track = (detail.gpsTrack || []) as [number, number][];
    if (track.length === 0) return;

    this.map = L.map(container, {
      zoomControl: true,
      scrollWheelZoom: true
    });

    const isDark = this.themeService.resolved() === 'dark';
    const tileUrl = isDark
      ? 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}{r}.png';
    L.tileLayer(tileUrl, {
      attribution: '&copy; OpenStreetMap &copy; CARTO',
      subdomains: 'abcd',
      maxZoom: 19
    }).addTo(this.map);

    const latlngs: L.LatLngExpression[] = track.map(p => [p[0], p[1]] as L.LatLngExpression);

    // Glow line
    L.polyline(latlngs, {
      color: '#8ffc2e',
      weight: 8,
      opacity: 0.35,
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(this.map);

    // Main line
    L.polyline(latlngs, {
      color: '#8ffc2e',
      weight: 4,
      opacity: 1,
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(this.map);

    // Start / end markers
    const startIcon = L.divIcon({
      className: '',
      html: '<div style="width:14px;height:14px;border-radius:50%;background:#fff;border:3px solid #8ffc2e;box-shadow:0 0 0 2px rgba(0,0,0,0.3)"></div>',
      iconSize: [14, 14],
      iconAnchor: [7, 7]
    });
    const endIcon = L.divIcon({
      className: '',
      html: '<div style="width:14px;height:14px;border-radius:50%;background:#8ffc2e;border:3px solid #fff;box-shadow:0 0 0 2px rgba(0,0,0,0.3)"></div>',
      iconSize: [14, 14],
      iconAnchor: [7, 7]
    });
    L.marker(latlngs[0], { icon: startIcon, interactive: false }).addTo(this.map);
    L.marker(latlngs[latlngs.length - 1], { icon: endIcon, interactive: false }).addTo(this.map);

    this.map.fitBounds(L.latLngBounds(latlngs), { padding: [24, 24] });
    setTimeout(() => this.map?.invalidateSize(), 100);
  }

  private destroyMap(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }
}

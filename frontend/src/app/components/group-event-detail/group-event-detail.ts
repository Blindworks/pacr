import { Component, OnInit, OnDestroy, AfterViewInit, inject, signal, ViewChild, ElementRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { GroupEventService, GroupEventDto } from '../../services/group-event.service';
import { ThemeService } from '../../services/theme.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';
import * as L from 'leaflet';

@Component({
  selector: 'app-group-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, ProOverlay],
  templateUrl: './group-event-detail.html',
  styleUrl: './group-event-detail.scss'
})
export class GroupEventDetail implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(GroupEventService);
  private readonly themeService = inject(ThemeService);
  readonly translate = inject(TranslateService);

  @ViewChild('eventMap') private mapElRef!: ElementRef<HTMLDivElement>;

  event = signal<GroupEventDto | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionLoading = signal(false);

  private map: L.Map | null = null;
  private tileLayer: L.TileLayer | null = null;
  private marker: L.Marker | null = null;
  private currentTheme: 'light' | 'dark' = 'dark';
  private mapInitialized = false;

  constructor() {
    effect(() => {
      const theme = this.themeService.resolved();
      if (this.map && theme !== this.currentTheme) {
        this.currentTheme = theme;
        this.updateTileLayer();
      }
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/community/groups']);
      return;
    }
    this.loadEvent(id);
  }

  private loadEvent(id: number): void {
    this.loading.set(true);
    this.eventService.getEventDetail(id).subscribe({
      next: event => {
        this.event.set(event);
        this.loading.set(false);
        setTimeout(() => this.tryInitMap(), 50);
      },
      error: () => { this.error.set(this.translate.instant('GROUP_EVENTS.LOAD_ERROR')); this.loading.set(false); }
    });
  }

  register(): void {
    const ev = this.event();
    if (!ev) return;
    this.actionLoading.set(true);
    this.eventService.registerForEvent(ev.id).subscribe({
      next: () => {
        this.event.set({ ...ev, isRegistered: true, currentParticipants: ev.currentParticipants + 1 });
        this.actionLoading.set(false);
      },
      error: () => { this.actionLoading.set(false); }
    });
  }

  cancelRegistration(): void {
    const ev = this.event();
    if (!ev) return;
    this.actionLoading.set(true);
    this.eventService.cancelRegistration(ev.id).subscribe({
      next: () => {
        this.event.set({ ...ev, isRegistered: false, currentParticipants: ev.currentParticipants - 1 });
        this.actionLoading.set(false);
      },
      error: () => { this.actionLoading.set(false); }
    });
  }

  goBack(): void {
    this.router.navigate(['/community/groups']);
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  formatCost(cents: number | null, currency: string): string {
    if (!cents || cents === 0) return this.translate.instant('GROUP_EVENTS.FREE');
    return (cents / 100).toLocaleString(this.translate.currentLang, {
      style: 'currency', currency: currency || 'EUR'
    });
  }

  getSpotsLeft(): number | null {
    const ev = this.event();
    if (!ev || !ev.maxParticipants) return null;
    return ev.maxParticipants - ev.currentParticipants;
  }

  getDifficultyClass(difficulty: string | null): string {
    if (!difficulty) return '';
    return `difficulty-${difficulty.toLowerCase()}`;
  }

  formatPace(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  private tryInitMap(): void {
    const ev = this.event();
    if (!ev || ev.latitude == null || ev.longitude == null) return;
    if (this.mapInitialized) return;
    if (!this.mapElRef?.nativeElement) return;

    this.currentTheme = this.themeService.resolved();
    this.initMap(ev.latitude, ev.longitude);
  }

  private initMap(lat: number, lng: number): void {
    const container = this.mapElRef.nativeElement;

    this.map = L.map(container, {
      center: [lat, lng],
      zoom: 14,
      zoomControl: false,
      attributionControl: false,
      dragging: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      touchZoom: false,
      boxZoom: false,
      keyboard: false
    });

    this.tileLayer = this.createTileLayer();
    this.tileLayer.addTo(this.map);
    this.placeMarker(lat, lng);
    this.map.invalidateSize();
    this.mapInitialized = true;
  }

  private placeMarker(lat: number, lng: number): void {
    if (!this.map) return;
    const icon = L.divIcon({
      className: 'location-marker',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });
    this.marker = L.marker([lat, lng], { icon }).addTo(this.map);
  }

  private createTileLayer(): L.TileLayer {
    const isDark = this.currentTheme === 'dark';
    const tileUrl = isDark
      ? 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}{r}.png';
    return L.tileLayer(tileUrl, {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19
    });
  }

  private updateTileLayer(): void {
    if (!this.map) return;
    if (this.tileLayer) {
      this.map.removeLayer(this.tileLayer);
    }
    this.tileLayer = this.createTileLayer();
    this.tileLayer.addTo(this.map);
  }
}

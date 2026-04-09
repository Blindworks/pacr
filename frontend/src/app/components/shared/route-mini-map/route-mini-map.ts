import {
  Component, Input, AfterViewInit, OnDestroy, OnChanges, SimpleChanges,
  ElementRef, ViewChild, ChangeDetectionStrategy, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../../services/theme.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-route-mini-map',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #mapEl class="mini-map"></div>`,
  styles: [`
    :host { display: block; width: 100%; height: 100%; position: relative; }
    .mini-map { width: 100%; height: 100%; background: #1a1a1a; }
    .mini-map :global(.leaflet-control-attribution) { display: none; }
  `]
})
export class RouteMiniMapComponent implements AfterViewInit, OnDestroy, OnChanges {
  @Input() track: [number, number][] | null = null;

  @ViewChild('mapEl', { static: true }) mapEl!: ElementRef<HTMLDivElement>;

  private readonly themeService = inject(ThemeService);
  private map: L.Map | null = null;
  private polyline: L.Polyline | null = null;

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['track'] && this.map) {
      this.drawRoute();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  private initMap(): void {
    if (!this.mapEl?.nativeElement || !this.track || this.track.length < 2) return;

    this.map = L.map(this.mapEl.nativeElement, {
      zoomControl: false,
      attributionControl: false,
      scrollWheelZoom: false,
      dragging: false,
      doubleClickZoom: false,
      touchZoom: false,
      keyboard: false,
      boxZoom: false,
      zoomAnimation: false,
      fadeAnimation: false
    });

    const isDark = this.themeService.resolved() === 'dark';
    const tileUrl = isDark
      ? 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}{r}.png';
    L.tileLayer(tileUrl, { subdomains: 'abcd', maxZoom: 19 }).addTo(this.map);

    this.drawRoute();
  }

  private drawRoute(): void {
    if (!this.map || !this.track || this.track.length < 2) return;

    if (this.polyline) {
      this.map.removeLayer(this.polyline);
      this.polyline = null;
    }

    const latlngs: L.LatLngExpression[] = this.track.map(([lat, lng]) => [lat, lng]);
    this.polyline = L.polyline(latlngs, {
      color: '#8ffc2e',
      weight: 3,
      opacity: 0.95,
      interactive: false
    }).addTo(this.map);

    const bounds = this.polyline.getBounds();
    if (bounds.isValid()) {
      this.map.fitBounds(bounds, { padding: [10, 10], animate: false });
    }
  }
}

import {
  Component, Input, Output, EventEmitter,
  AfterViewInit, OnDestroy, OnChanges, SimpleChanges,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { GpsStreamDto } from '../../services/activity.service';
import * as L from 'leaflet';

// Make L available globally for leaflet-hotline UMD module
(window as any).L = L;

// leaflet-hotline extends L with L.hotline() via side-effect
import 'leaflet-hotline';

@Component({
  selector: 'app-activity-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-map.html',
  styleUrl: './activity-map.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ActivityMapComponent implements AfterViewInit, OnDestroy, OnChanges {
  @Input({ required: true }) gpsData!: GpsStreamDto;
  @Input() colorMode: 'pace' | 'hr' = 'pace';
  @Output() colorModeChange = new EventEmitter<'pace' | 'hr'>();

  readonly mapContainerId = 'activity-map-' + Math.random().toString(36).slice(2, 8);

  private map: L.Map | null = null;
  private hotlineLayer: any = null;
  private clickLayer: L.Polyline | null = null;
  private markerGroup: L.LayerGroup | null = null;

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['colorMode'] && !changes['colorMode'].firstChange && this.map) {
      this.drawRoute();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  setColorMode(mode: 'pace' | 'hr'): void {
    this.colorModeChange.emit(mode);
  }

  private initMap(): void {
    const container = document.getElementById(this.mapContainerId);
    if (!container) return;

    this.map = L.map(container, {
      zoomControl: false,
      attributionControl: true,
      scrollWheelZoom: false,
      dragging: true,
      doubleClickZoom: false,
      touchZoom: true
    });

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19
    }).addTo(this.map);

    this.markerGroup = L.layerGroup().addTo(this.map);
    this.drawRoute();
    this.addKmMarkers();
  }

  private drawRoute(): void {
    if (!this.map || !this.gpsData?.latlng?.length) return;

    // Remove previous layers
    if (this.hotlineLayer) {
      this.map.removeLayer(this.hotlineLayer);
      this.hotlineLayer = null;
    }
    if (this.clickLayer) {
      this.map.removeLayer(this.clickLayer);
      this.clickLayer = null;
    }

    const data = this.gpsData;
    const values = this.colorMode === 'hr' && data.hasHeartRate
      ? data.heartRate
      : data.paceSecondsPerKm;

    // Build hotline data: [lat, lng, value]
    const hotlineData: [number, number, number][] = [];
    const latlngs: L.LatLngExpression[] = [];

    for (let i = 0; i < data.latlng.length; i++) {
      const [lat, lng] = data.latlng[i];
      const val = values?.[i] ?? 0;
      hotlineData.push([lat, lng, val]);
      latlngs.push([lat, lng]);
    }

    // Compute min/max for the selected metric
    const validValues = hotlineData.map(d => d[2]).filter(v => v > 0);
    const min = validValues.length ? Math.min(...validValues) : 0;
    const max = validValues.length ? Math.max(...validValues) : 1;

    // Gradient palette: green (easy) → yellow (moderate) → red (intense)
    const palette: Record<number, string> = {
      0.0: '#3fb950',
      0.5: '#d29922',
      1.0: '#f85149'
    };

    try {
      this.hotlineLayer = (L as any).hotline(hotlineData, {
        min,
        max,
        palette,
        weight: 5,
        outlineWidth: 1,
        outlineColor: 'rgba(0, 0, 0, 0.3)'
      }).addTo(this.map);
    } catch {
      // Fallback to regular polyline if hotline fails
      this.hotlineLayer = L.polyline(latlngs, {
        color: '#b9f20d',
        weight: 4,
        opacity: 0.9
      }).addTo(this.map);
    }

    // Invisible click layer for interaction
    this.clickLayer = L.polyline(latlngs, {
      weight: 20,
      opacity: 0,
      interactive: true
    }).addTo(this.map);

    this.clickLayer.on('click', (e: L.LeafletMouseEvent) => this.onRouteClick(e));

    // Fit bounds
    const bounds = L.latLngBounds(latlngs as L.LatLngExpression[]);
    if (bounds.isValid()) {
      this.map!.fitBounds(bounds, { padding: [30, 30], animate: false });
    }

    // Add start/end markers
    if (latlngs.length > 1) {
      const startIcon = L.divIcon({
        className: '',
        html: '<div style="width:12px;height:12px;border-radius:50%;background:#fff;border:3px solid #b9f20d;box-shadow:0 2px 6px rgba(0,0,0,0.4)"></div>',
        iconSize: [12, 12],
        iconAnchor: [6, 6]
      });
      const endIcon = L.divIcon({
        className: '',
        html: '<div style="width:12px;height:12px;border-radius:50%;background:#0d1117;border:3px solid #b9f20d;box-shadow:0 2px 6px rgba(0,0,0,0.4)"></div>',
        iconSize: [12, 12],
        iconAnchor: [6, 6]
      });
      L.marker(latlngs[0] as L.LatLngExpression, { icon: startIcon, interactive: false }).addTo(this.map!);
      L.marker(latlngs[latlngs.length - 1] as L.LatLngExpression, { icon: endIcon, interactive: false }).addTo(this.map!);
    }
  }

  private addKmMarkers(): void {
    if (!this.map || !this.markerGroup || !this.gpsData?.distance?.length) return;
    this.markerGroup.clearLayers();

    const data = this.gpsData;
    let nextKm = 1000; // in meters

    for (let i = 0; i < data.distance.length; i++) {
      if (data.distance[i] >= nextKm) {
        const km = Math.round(nextKm / 1000);
        const [lat, lng] = data.latlng[i];
        const icon = L.divIcon({
          className: '',
          html: `<div class="km-marker">${km}</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12]
        });
        L.marker([lat, lng], { icon, interactive: false }).addTo(this.markerGroup!);
        nextKm += 1000;
      }
    }
  }

  private onRouteClick(e: L.LeafletMouseEvent): void {
    if (!this.gpsData?.latlng?.length) return;

    // Find nearest point
    const clickLat = e.latlng.lat;
    const clickLng = e.latlng.lng;
    let minDist = Infinity;
    let nearestIdx = 0;

    for (let i = 0; i < this.gpsData.latlng.length; i++) {
      const [lat, lng] = this.gpsData.latlng[i];
      const dist = (lat - clickLat) ** 2 + (lng - clickLng) ** 2;
      if (dist < minDist) {
        minDist = dist;
        nearestIdx = i;
      }
    }

    const data = this.gpsData;
    const [lat, lng] = data.latlng[nearestIdx];
    const distKm = (data.distance[nearestIdx] / 1000).toFixed(2);

    let content = `<div style="font-family:'Inter',sans-serif;font-size:12px;line-height:1.6">`;
    content += `<strong style="color:#b9f20d">${distKm} km</strong><br>`;

    if (data.hasPace && data.paceSecondsPerKm?.[nearestIdx]) {
      const pace = data.paceSecondsPerKm[nearestIdx]!;
      const m = Math.floor(pace / 60);
      const s = Math.round(pace % 60);
      content += `Pace: ${m}:${String(s).padStart(2, '0')} /km<br>`;
    }
    if (data.hasHeartRate && data.heartRate?.[nearestIdx]) {
      content += `HF: ${data.heartRate[nearestIdx]} bpm<br>`;
    }
    if (data.hasAltitude && data.altitude?.[nearestIdx] != null) {
      content += `Höhe: ${Math.round(data.altitude[nearestIdx]!)} m`;
    }
    content += `</div>`;

    L.popup({ closeButton: true, offset: [0, -5] })
      .setLatLng([lat, lng])
      .setContent(content)
      .openOn(this.map!);
  }
}

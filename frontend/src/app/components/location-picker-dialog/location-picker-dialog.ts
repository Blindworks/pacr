import {
  Component, ViewChild, ElementRef, Output, EventEmitter,
  inject, effect, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ThemeService } from '../../services/theme.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-location-picker-dialog',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './location-picker-dialog.html',
  styleUrl: './location-picker-dialog.scss'
})
export class LocationPickerDialogComponent {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;
  @Output() confirmed = new EventEmitter<{ lat: number; lng: number }>();

  private readonly themeService = inject(ThemeService);

  selectedLat = signal<number | null>(null);
  selectedLng = signal<number | null>(null);

  private map: L.Map | null = null;
  private tileLayer: L.TileLayer | null = null;
  private marker: L.Marker | null = null;
  private currentTheme: 'light' | 'dark' = 'dark';
  private initialized = false;

  constructor() {
    effect(() => {
      const theme = this.themeService.resolved();
      if (this.map && theme !== this.currentTheme) {
        this.currentTheme = theme;
        this.updateTileLayer();
      }
    });
  }

  open(lat?: number | null, lon?: number | null): void {
    this.currentTheme = this.themeService.resolved();
    this.dialogRef.nativeElement.showModal();

    const hasCoords = lat != null && lon != null;
    const centerLat = hasCoords ? lat! : 51.16;
    const centerLng = hasCoords ? lon! : 10.45;
    const zoom = hasCoords ? 14 : 6;

    if (hasCoords) {
      this.selectedLat.set(lat!);
      this.selectedLng.set(lon!);
    } else {
      this.selectedLat.set(null);
      this.selectedLng.set(null);
    }

    // Delay to let dialog render before map init
    setTimeout(() => {
      if (!this.initialized) {
        this.initMap(centerLat, centerLng, zoom);
        this.initialized = true;
      } else {
        this.map!.setView([centerLat, centerLng], zoom);
        this.map!.invalidateSize();
      }

      if (hasCoords) {
        this.placeMarker(centerLat, centerLng);
      } else if (this.marker) {
        this.map!.removeLayer(this.marker);
        this.marker = null;
      }
    }, 50);
  }

  close(): void {
    this.dialogRef.nativeElement.close();
  }

  confirm(): void {
    const lat = this.selectedLat();
    const lng = this.selectedLng();
    if (lat != null && lng != null) {
      this.confirmed.emit({ lat, lng });
    }
    this.close();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }

  private initMap(lat: number, lng: number, zoom: number): void {
    const container = this.dialogRef.nativeElement.querySelector('.picker-map') as HTMLElement;
    if (!container) return;

    this.map = L.map(container, {
      center: [lat, lng],
      zoom,
      zoomControl: true,
      attributionControl: true
    });

    this.tileLayer = this.createTileLayer();
    this.tileLayer.addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.placeMarker(e.latlng.lat, e.latlng.lng);
      this.selectedLat.set(Math.round(e.latlng.lat * 1000000) / 1000000);
      this.selectedLng.set(Math.round(e.latlng.lng * 1000000) / 1000000);
    });
  }

  private placeMarker(lat: number, lng: number): void {
    if (!this.map) return;

    if (this.marker) {
      this.marker.setLatLng([lat, lng]);
    } else {
      const icon = L.divIcon({
        className: 'location-marker',
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      });
      this.marker = L.marker([lat, lng], { icon }).addTo(this.map);
    }
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

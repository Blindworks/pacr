import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { GroupEventService, GroupEventDto } from '../../services/group-event.service';
import { UserService } from '../../services/user.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';

@Component({
  selector: 'app-group-events',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule, ProOverlay],
  templateUrl: './group-events.html',
  styleUrl: './group-events.scss'
})
export class GroupEvents implements OnInit {
  private readonly eventService = inject(GroupEventService);
  private readonly userService = inject(UserService);
  readonly translate = inject(TranslateService);

  events = signal<GroupEventDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  activeTab = signal<'near' | 'upcoming' | 'mine'>('near');
  radiusKm = signal(10);
  userLat = signal<number | null>(null);
  userLon = signal<number | null>(null);

  ngOnInit(): void {
    this.getUserLocation();
  }

  private getUserLocation(): void {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        pos => {
          this.userLat.set(pos.coords.latitude);
          this.userLon.set(pos.coords.longitude);
          this.loadEvents();
        },
        () => {
          this.userLat.set(48.137154);
          this.userLon.set(11.576124);
          this.loadEvents();
        }
      );
    } else {
      this.userLat.set(48.137154);
      this.userLon.set(11.576124);
      this.loadEvents();
    }
  }

  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);

    const tab = this.activeTab();

    if (tab === 'near') {
      const lat = this.userLat();
      const lon = this.userLon();
      if (lat === null || lon === null) return;
      this.eventService.getNearbyEvents(lat, lon, this.radiusKm()).subscribe({
        next: events => { this.events.set(events); this.loading.set(false); },
        error: () => { this.error.set(this.translate.instant('GROUP_EVENTS.LOAD_ERROR')); this.loading.set(false); }
      });
    } else if (tab === 'upcoming') {
      this.eventService.getUpcomingEvents().subscribe({
        next: events => { this.events.set(events); this.loading.set(false); },
        error: () => { this.error.set(this.translate.instant('GROUP_EVENTS.LOAD_ERROR')); this.loading.set(false); }
      });
    } else {
      this.eventService.getMyRegistrations().subscribe({
        next: events => { this.events.set(events); this.loading.set(false); },
        error: () => { this.error.set(this.translate.instant('GROUP_EVENTS.LOAD_ERROR')); this.loading.set(false); }
      });
    }
  }

  onTabChange(tab: 'near' | 'upcoming' | 'mine'): void {
    this.activeTab.set(tab);
    this.loadEvents();
  }

  onRadiusChange(value: number): void {
    this.radiusKm.set(value);
    if (this.activeTab() === 'near') {
      this.loadEvents();
    }
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
    });
  }

  formatCost(cents: number | null, currency: string): string {
    if (!cents || cents === 0) return this.translate.instant('GROUP_EVENTS.FREE');
    return (cents / 100).toLocaleString(this.translate.currentLang, {
      style: 'currency', currency: currency || 'EUR'
    });
  }

  getDifficultyClass(difficulty: string | null): string {
    if (!difficulty) return '';
    return `difficulty-${difficulty.toLowerCase()}`;
  }

  getSpotsLeft(event: GroupEventDto): number | null {
    if (!event.maxParticipants) return null;
    return event.maxParticipants - event.currentParticipants;
  }
}

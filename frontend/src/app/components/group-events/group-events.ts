import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { GroupEventService, GroupEventDto } from '../../services/group-event.service';
import { UserService } from '../../services/user.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';
import { EventImageComponent } from '../event-image/event-image';

@Component({
  selector: 'app-group-events',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule, ProOverlay, EventImageComponent],
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
  paceFilterSeconds = signal<number | null>(null);

  filteredEvents = computed(() => {
    const pace = this.paceFilterSeconds();
    if (!pace) return this.events();
    return this.events().filter(e => {
      if (!e.paceMinSecondsPerKm || !e.paceMaxSecondsPerKm) return true;
      return pace >= e.paceMinSecondsPerKm && pace <= e.paceMaxSecondsPerKm;
    });
  });

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

  formatPace(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  onPaceFilterChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.trim();
    if (!value) {
      this.paceFilterSeconds.set(null);
      return;
    }
    const match = value.match(/^(\d{1,2}):(\d{2})$/);
    if (match) {
      this.paceFilterSeconds.set(parseInt(match[1]) * 60 + parseInt(match[2]));
    }
  }

  clearPaceFilter(): void {
    this.paceFilterSeconds.set(null);
  }

  trackEvent(index: number, event: GroupEventDto): string {
    return event.occurrenceDate ? `${event.id}-${event.occurrenceDate}` : `${event.id}`;
  }

  private todayStr = (() => {
    const t = new Date();
    return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`;
  })();

  isToday(event: GroupEventDto): boolean {
    const raw = event.occurrenceDate || event.eventDate;
    if (!raw) return false;
    return raw.substring(0, 10) === this.todayStr;
  }
}

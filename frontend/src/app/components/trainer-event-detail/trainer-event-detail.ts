import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TrainerEventService, GroupEventRegistrationDto } from '../../services/trainer-event.service';
import { GroupEventDto } from '../../services/group-event.service';

@Component({
  selector: 'app-trainer-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './trainer-event-detail.html',
  styleUrl: './trainer-event-detail.scss'
})
export class TrainerEventDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trainerService = inject(TrainerEventService);
  readonly translate = inject(TranslateService);

  event = signal<GroupEventDto | null>(null);
  participants = signal<GroupEventRegistrationDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  actionLoading = signal(false);

  // Occurrences
  occurrences = signal<GroupEventDto[]>([]);
  occurrencesLoading = signal(false);
  occurrencesExpanded = signal(false);

  // Extend
  showExtendMenu = signal(false);
  extendLoading = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/trainer/events']);
      return;
    }
    this.loadEvent(id);
  }

  private loadEvent(id: number): void {
    this.loading.set(true);
    this.trainerService.getTrainerEvents().subscribe({
      next: events => {
        const event = events.find(e => e.id === id);
        if (event) {
          this.event.set(event);
          this.loadParticipants(id);
          if (event.isRecurring) {
            this.loadOccurrences(event.id);
          }
        } else {
          this.error.set(this.translate.instant('TRAINER_EVENTS.NOT_FOUND'));
          this.loading.set(false);
        }
      },
      error: () => {
        this.error.set(this.translate.instant('TRAINER_EVENTS.LOAD_ERROR'));
        this.loading.set(false);
      }
    });
  }

  private loadParticipants(eventId: number): void {
    this.trainerService.getParticipants(eventId).subscribe({
      next: participants => {
        this.participants.set(participants);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private loadOccurrences(eventId: number): void {
    this.occurrencesLoading.set(true);
    const today = new Date();
    const to = new Date();
    to.setDate(to.getDate() + 90);
    const fromStr = today.toISOString().split('T')[0];
    const toStr = to.toISOString().split('T')[0];

    this.trainerService.getOccurrences(eventId, fromStr, toStr).subscribe({
      next: occ => {
        this.occurrences.set(occ);
        this.occurrencesLoading.set(false);
      },
      error: () => {
        this.occurrencesLoading.set(false);
      }
    });
  }

  publishEvent(): void {
    const ev = this.event();
    if (!ev) return;
    this.actionLoading.set(true);
    this.trainerService.publishEvent(ev.id).subscribe({
      next: updated => {
        this.event.set(updated);
        this.actionLoading.set(false);
      },
      error: () => { this.actionLoading.set(false); }
    });
  }

  cancelEvent(): void {
    const ev = this.event();
    if (!ev) return;
    this.actionLoading.set(true);
    this.trainerService.cancelEvent(ev.id).subscribe({
      next: () => {
        this.event.set({ ...ev, status: 'CANCELLED' });
        this.actionLoading.set(false);
      },
      error: () => { this.actionLoading.set(false); }
    });
  }

  deleteEvent(): void {
    const ev = this.event();
    if (!ev) return;
    this.actionLoading.set(true);
    this.trainerService.deleteEvent(ev.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.router.navigate(['/trainer/events']);
      },
      error: () => { this.actionLoading.set(false); }
    });
  }

  toggleOccurrences(): void {
    this.occurrencesExpanded.update(v => !v);
  }

  toggleExtendMenu(): void {
    this.showExtendMenu.update(v => !v);
  }

  shouldShowExtendButton(ev: GroupEventDto): boolean {
    if (!ev.isRecurring || !ev.recurrenceEndDate) return false;
    const endDate = new Date(ev.recurrenceEndDate);
    const fourWeeksFromNow = new Date();
    fourWeeksFromNow.setDate(fourWeeksFromNow.getDate() + 28);
    return endDate <= fourWeeksFromNow;
  }

  extendRecurrence(weeks: number): void {
    const ev = this.event();
    if (!ev || !ev.recurrenceEndDate) return;
    const currentEnd = new Date(ev.recurrenceEndDate);
    currentEnd.setDate(currentEnd.getDate() + weeks * 7);
    const newEndStr = currentEnd.toISOString().split('T')[0];
    this.doExtend(newEndStr);
  }

  extendRecurrenceCustom(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.value) return;
    this.doExtend(input.value);
  }

  private doExtend(newEndDate: string): void {
    const ev = this.event();
    if (!ev) return;
    this.extendLoading.set(true);
    this.trainerService.updateEvent(ev.id, { recurrenceEndDate: newEndDate }).subscribe({
      next: updated => {
        this.event.set(updated);
        this.showExtendMenu.set(false);
        this.extendLoading.set(false);
        this.loadOccurrences(ev.id);
      },
      error: () => {
        this.extendLoading.set(false);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/trainer/events']);
  }

  // --- Formatting helpers ---

  formatPace(seconds: number | null): string {
    if (!seconds) return '';
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  }

  formatPaceRange(ev: GroupEventDto): string {
    const min = this.formatPace(ev.paceMinSecondsPerKm);
    const max = this.formatPace(ev.paceMaxSecondsPerKm);
    if (min && max) return `${min} – ${max} min/km`;
    if (min) return `${min} min/km`;
    if (max) return `${max} min/km`;
    return '';
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  formatOccurrenceDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
    });
  }

  formatCost(cents: number | null, currency: string): string {
    if (!cents || cents === 0) return this.translate.instant('TRAINER_EVENTS.FREE');
    return (cents / 100).toLocaleString(this.translate.currentLang, {
      style: 'currency', currency: currency || 'EUR'
    });
  }

  formatRegisteredAt(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  isSeriesEnded(ev: GroupEventDto): boolean {
    if (!ev.recurrenceEndDate) return false;
    return new Date(ev.recurrenceEndDate) < new Date();
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getDifficultyClass(difficulty: string | null): string {
    if (!difficulty) return '';
    return `difficulty-${difficulty.toLowerCase()}`;
  }
}

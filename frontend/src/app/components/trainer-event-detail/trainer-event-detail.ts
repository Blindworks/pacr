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

  goBack(): void {
    this.router.navigate(['/trainer/events']);
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
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

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getDifficultyClass(difficulty: string | null): string {
    if (!difficulty) return '';
    return `difficulty-${difficulty.toLowerCase()}`;
  }
}

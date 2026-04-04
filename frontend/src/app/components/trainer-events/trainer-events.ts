import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TrainerEventService } from '../../services/trainer-event.service';
import { GroupEventDto } from '../../services/group-event.service';

@Component({
  selector: 'app-trainer-events',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './trainer-events.html',
  styleUrl: './trainer-events.scss'
})
export class TrainerEvents implements OnInit {
  private readonly trainerService = inject(TrainerEventService);
  readonly translate = inject(TranslateService);

  events = signal<GroupEventDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);
    this.trainerService.getTrainerEvents().subscribe({
      next: events => { this.events.set(events); this.loading.set(false); },
      error: () => { this.error.set(this.translate.instant('TRAINER_EVENTS.LOAD_ERROR')); this.loading.set(false); }
    });
  }

  publishEvent(event: GroupEventDto, e: Event): void {
    e.preventDefault();
    e.stopPropagation();
    this.trainerService.publishEvent(event.id).subscribe({
      next: updated => {
        this.events.update(list => list.map(ev => ev.id === updated.id ? updated : ev));
      }
    });
  }

  cancelEvent(event: GroupEventDto, e: Event): void {
    e.preventDefault();
    e.stopPropagation();
    this.trainerService.cancelEvent(event.id).subscribe({
      next: () => {
        this.events.update(list => list.map(ev => ev.id === event.id ? { ...ev, status: 'CANCELLED' } : ev));
      }
    });
  }

  deleteEvent(event: GroupEventDto, e: Event): void {
    e.preventDefault();
    e.stopPropagation();
    this.trainerService.deleteEvent(event.id).subscribe({
      next: () => {
        this.events.update(list => list.filter(ev => ev.id !== event.id));
      }
    });
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this.translate.currentLang, {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
    });
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }
}

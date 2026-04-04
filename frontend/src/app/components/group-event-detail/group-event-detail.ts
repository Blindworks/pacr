import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { GroupEventService, GroupEventDto } from '../../services/group-event.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';

@Component({
  selector: 'app-group-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, ProOverlay],
  templateUrl: './group-event-detail.html',
  styleUrl: './group-event-detail.scss'
})
export class GroupEventDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(GroupEventService);
  readonly translate = inject(TranslateService);

  event = signal<GroupEventDto | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionLoading = signal(false);

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
      next: event => { this.event.set(event); this.loading.set(false); },
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
}

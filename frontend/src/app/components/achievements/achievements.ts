import { Component, OnInit, inject, signal, ElementRef, ViewChild } from '@angular/core';
import { AchievementService, Achievement } from '../../services/achievement.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [ProOverlay],
  templateUrl: './achievements.html',
  styleUrl: './achievements.scss'
})
export class Achievements implements OnInit {
  private readonly achievementService = inject(AchievementService);

  achievements = signal<Achievement[]>([]);
  loading = signal(true);
  selectedAchievement = signal<Achievement | null>(null);

  @ViewChild('detailDialog') detailDialogRef!: ElementRef<HTMLDialogElement>;

  readonly categories = [
    { key: 'DISTANCE', label: 'Distance Milestones', icon: 'directions_run' },
    { key: 'STREAK', label: 'Training Streaks', icon: 'local_fire_department' },
    { key: 'PR', label: 'Personal Records', icon: 'emoji_events' },
    { key: 'PLAN_COMPLETION', label: 'Plan Completion', icon: 'task_alt' }
  ];

  ngOnInit(): void {
    this.achievementService.getAll().subscribe({
      next: data => {
        this.achievements.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getPermanentByCategory(category: string): Achievement[] {
    return this.achievements().filter(a => a.category === category && !a.timeBound);
  }

  getTimeBoundActive(): Achievement[] {
    return this.achievements().filter(a => a.timeBound && a.active);
  }

  getTimeBoundExpired(): Achievement[] {
    return this.achievements().filter(a => a.timeBound && a.expired);
  }

  openDetail(achievement: Achievement): void {
    this.selectedAchievement.set(achievement);
    this.detailDialogRef?.nativeElement?.showModal();
  }

  closeDetail(): void {
    this.detailDialogRef?.nativeElement?.close();
    this.selectedAchievement.set(null);
  }

  onDialogClick(event: MouseEvent): void {
    const dialog = this.detailDialogRef.nativeElement;
    const rect = dialog.getBoundingClientRect();
    if (
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom
    ) {
      this.closeDetail();
    }
  }

  formatProgress(a: Achievement): string {
    if (a.threshold <= 1) {
      return a.unlocked ? 'Completed' : 'Not yet';
    }
    const current = a.currentValue ?? 0;
    if (a.category === 'DISTANCE') {
      return `${Math.round(current)} / ${Math.round(a.threshold)} km`;
    }
    return `${Math.round(current)} / ${Math.round(a.threshold)}`;
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatTimeframe(a: Achievement): string {
    const from = a.validFrom ? this.formatDate(a.validFrom) : '';
    const until = a.validUntil ? this.formatDate(a.validUntil) : '';
    if (from && until) return `${from} – ${until}`;
    if (from) return `Ab ${from}`;
    if (until) return `Bis ${until}`;
    return '';
  }
}

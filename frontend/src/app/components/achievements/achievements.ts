import { Component, OnInit, inject, signal, ElementRef, ViewChild } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AchievementService, Achievement } from '../../services/achievement.service';
import { ProOverlay } from '../shared/pro-overlay/pro-overlay';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [ProOverlay, TranslateModule],
  templateUrl: './achievements.html',
  styleUrl: './achievements.scss'
})
export class Achievements implements OnInit {
  private readonly achievementService = inject(AchievementService);
  private readonly translate = inject(TranslateService);

  achievements = signal<Achievement[]>([]);
  loading = signal(true);
  selectedAchievement = signal<Achievement | null>(null);

  @ViewChild('detailDialog') detailDialogRef!: ElementRef<HTMLDialogElement>;

  readonly categories = [
    { key: 'DISTANCE', labelKey: 'ACHIEVEMENTS.CAT_DISTANCE', icon: 'directions_run' },
    { key: 'STREAK', labelKey: 'ACHIEVEMENTS.CAT_STREAK', icon: 'local_fire_department' },
    { key: 'PR', labelKey: 'ACHIEVEMENTS.CAT_PR', icon: 'emoji_events' },
    { key: 'PLAN_COMPLETION', labelKey: 'ACHIEVEMENTS.CAT_PLAN', icon: 'task_alt' }
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
      return a.unlocked
        ? this.translate.instant('ACHIEVEMENTS.COMPLETED')
        : this.translate.instant('ACHIEVEMENTS.NOT_YET');
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
    if (from) return `${this.translate.instant('ACHIEVEMENTS.FROM')} ${from}`;
    if (until) return `${this.translate.instant('ACHIEVEMENTS.UNTIL')} ${until}`;
    return '';
  }
}

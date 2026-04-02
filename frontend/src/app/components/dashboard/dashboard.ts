import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DashboardService, DashboardData } from '../../services/dashboard.service';
import { AchievementService, StreakInfo } from '../../services/achievement.service';
import { AcwrInfoDialogService } from '../../services/acwr-info-dialog.service';
import { AcwrInfoDialog } from '../acwr-info-dialog/acwr-info-dialog';
import { StrainInfoDialogService } from '../../services/strain-info-dialog.service';
import { StrainInfoDialog } from '../strain-info-dialog/strain-info-dialog';
import { ReadinessInfoDialogService } from '../../services/readiness-info-dialog.service';
import { ReadinessInfoDialog } from '../readiness-info-dialog/readiness-info-dialog';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, AcwrInfoDialog, StrainInfoDialog, ReadinessInfoDialog],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly achievementService = inject(AchievementService);
  private readonly translate = inject(TranslateService);
  protected readonly acwrInfoService = inject(AcwrInfoDialogService);
  protected readonly strainInfoService = inject(StrainInfoDialogService);
  protected readonly readinessInfoService = inject(ReadinessInfoDialogService);

  data: DashboardData | null = null;
  profileError = signal<string | null>(null);
  missingFields = signal<string[]>([]);
  streak = signal<StreakInfo | null>(null);

  private readonly FIELD_LABELS: Record<string, string> = {
    firstName: 'DASHBOARD.FIELD_FIRST_NAME',
    lastName: 'DASHBOARD.FIELD_LAST_NAME',
    dateOfBirth: 'DASHBOARD.FIELD_DOB',
    heightCm: 'DASHBOARD.FIELD_HEIGHT',
    weightKg: 'DASHBOARD.FIELD_WEIGHT',
    maxHeartRate: 'DASHBOARD.FIELD_MAX_HR',
    hrRest: 'DASHBOARD.FIELD_REST_HR',
    gender: 'DASHBOARD.FIELD_GENDER'
  };

  ngOnInit(): void {
    this.dashboardService.getDashboard().subscribe({
      next: data => { this.data = data; },
      error: err => {
        if (err.status === 400 && err.error?.missingFields) {
          this.missingFields.set(
            (err.error.missingFields as string[]).map(f => this.translate.instant(this.FIELD_LABELS[f] ?? f))
          );
          this.profileError.set(this.translate.instant('DASHBOARD.PROFILE_INCOMPLETE'));
        } else {
          console.error('Dashboard load failed', err);
        }
      }
    });

    this.achievementService.getStreak().subscribe({
      next: s => this.streak.set(s),
      error: () => {}
    });
  }

  // ── Load Trend Bar Chart (aktuelle Woche Mo–So) ──────────────────
  barPoints(): Array<{ height: number; label: string; active: boolean; future: boolean; strain: number; distanceKm: number }> {
    const trend = this.data?.loadTrend ?? [];
    const todayDate = new Date();
    const isoFmt = new Intl.DateTimeFormat('sv-SE');
    const today = isoFmt.format(todayDate);

    // Montag der aktuellen Woche berechnen (JS: 0=So, 1=Mo, ...)
    const dayOfWeek = todayDate.getDay();
    const diffToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
    const monday = new Date(todayDate);
    monday.setDate(todayDate.getDate() - diffToMonday);

    // 7 Tage Mo-So als ISO-Strings
    const weekDates: string[] = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      weekDates.push(isoFmt.format(d));
    }

    // Trend-Daten als Map fuer schnellen Zugriff (API-Daten auf lokales Datumsformat normalisieren)
    const trendMap = new Map(trend.map(p => [isoFmt.format(new Date(p.date)), p]));

    const weekPoints = weekDates.map(date => {
      const p = trendMap.get(date);
      return {
        date,
        strain21: p?.strain21 ?? 0,
        distanceKm: p?.distanceKm ?? 0
      };
    });

    const max = Math.max(...weekPoints.map(p => p.distanceKm), 50);
    const WEEK_LABELS = [
      this.translate.instant('DAYS_SHORT.MON'),
      this.translate.instant('DAYS_SHORT.TUE'),
      this.translate.instant('DAYS_SHORT.WED'),
      this.translate.instant('DAYS_SHORT.THU'),
      this.translate.instant('DAYS_SHORT.FRI'),
      this.translate.instant('DAYS_SHORT.SAT'),
      this.translate.instant('DAYS_SHORT.SUN')
    ];

    return weekPoints.map((p, i) => ({
      height: p.date > today ? 0 : Math.max(4, Math.round(p.distanceKm / max * 100)),
      label: WEEK_LABELS[i],
      active: p.date === today,
      future: p.date > today,
      strain: p.strain21,
      distanceKm: p.distanceKm
    }));
  }

  // ── VO2 Max Gauge ────────────────────────────────────────────────
  gaugeOffset(): number {
    const vo2 = this.data?.vo2max ?? 0;
    const pct = Math.min(1, Math.max(0, (vo2 - 25) / 45));
    return Math.round(502 * (1 - pct));
  }

  gaugeClass(): 'good' | 'warn' | 'bad' {
    const vo2 = this.data?.vo2max ?? 0;
    if (vo2 >= 50) return 'good';
    if (vo2 >= 40) return 'warn';
    return 'bad';
  }

  gaugeSublabel(): string {
    const vo2 = this.data?.vo2max ?? 0;
    if (vo2 >= 60) return this.translate.instant('DASHBOARD.VO2_EXCELLENT');
    if (vo2 >= 55) return this.translate.instant('DASHBOARD.VO2_VERY_GOOD');
    if (vo2 >= 50) return this.translate.instant('DASHBOARD.VO2_GOOD');
    if (vo2 >= 45) return this.translate.instant('DASHBOARD.VO2_AVERAGE');
    if (vo2 >= 40) return this.translate.instant('DASHBOARD.VO2_BELOW_AVG');
    return this.translate.instant('DASHBOARD.VO2_LOW');
  }

  // ── Load Status ──────────────────────────────────────────────────
  loadFlagClass(): string {
    const flag = this.data?.loadStatus?.flag ?? 'BLUE';
    const map: Record<string, string> = { GREEN: 'good', ORANGE: 'warn', RED: 'bad', BLUE: 'info' };
    return map[flag] ?? 'info';
  }

  loadFlagLabel(): string {
    const flag = this.data?.loadStatus?.flag ?? 'BLUE';
    const map: Record<string, string> = {
      GREEN: 'DASHBOARD.LOAD_OPTIMAL',
      ORANGE: 'DASHBOARD.LOAD_CAUTION',
      RED: 'DASHBOARD.LOAD_OVERLOAD',
      BLUE: 'DASHBOARD.LOAD_BUILDING'
    };
    return this.translate.instant(map[flag] ?? flag);
  }

  // ── Training Progress ────────────────────────────────────────────
  progressPct(completed: number, total: number): number {
    return total > 0 ? Math.round(completed / total * 100) : 0;
  }

  // ── Last Run ─────────────────────────────────────────────────────
  hasLastRun(): boolean {
    return this.data?.lastRun != null && (this.data.lastRun.strain21 > 0 || (this.data.lastRun.coachBullets?.length ?? 0) > 0);
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

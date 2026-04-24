import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DashboardService, DashboardData } from '../../services/dashboard.service';
import { UserService } from '../../services/user.service';
import { AsthmaService, BioWeatherDto } from '../../services/asthma.service';
import { CycleSettingsService, CycleStatusDto } from '../../services/cycle-settings.service';
import { AcwrInfoDialogService } from '../../services/acwr-info-dialog.service';
import { AcwrInfoDialog } from '../acwr-info-dialog/acwr-info-dialog';
import { StrainInfoDialogService } from '../../services/strain-info-dialog.service';
import { StrainInfoDialog } from '../strain-info-dialog/strain-info-dialog';
import { ReadinessInfoDialogService } from '../../services/readiness-info-dialog.service';
import { ReadinessInfoDialog } from '../readiness-info-dialog/readiness-info-dialog';
import { NewStravaActivityDialogService } from '../../services/new-strava-activity-dialog.service';
import { NewStravaActivityDialog } from '../new-strava-activity-dialog/new-strava-activity-dialog';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, AcwrInfoDialog, StrainInfoDialog, ReadinessInfoDialog, NewStravaActivityDialog],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly userService = inject(UserService);
  private readonly asthmaService = inject(AsthmaService);
  private readonly cycleSettingsService = inject(CycleSettingsService);
  private readonly translate = inject(TranslateService);
  protected readonly acwrInfoService = inject(AcwrInfoDialogService);
  protected readonly strainInfoService = inject(StrainInfoDialogService);
  protected readonly readinessInfoService = inject(ReadinessInfoDialogService);
  private readonly newStravaActivityDialog = inject(NewStravaActivityDialogService);

  data: DashboardData | null = null;
  profileError = signal<string | null>(null);
  missingFields = signal<string[]>([]);
  bioWeather = signal<BioWeatherDto | null>(null);
  cycleStatus = signal<CycleStatusDto | null>(null);

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

    const loadOptional = () => {
      const user = this.userService.currentUser();
      if (!user) return;
      if (user.asthmaTrackingEnabled) {
        this.asthmaService.getEnvironment(user.dwdRegionId ?? undefined).subscribe({
          next: env => this.bioWeather.set(env),
          error: err => console.warn('Asthma environment load failed', err)
        });
      }
      if (user.cycleTrackingEnabled) {
        this.cycleSettingsService.getStatus().subscribe({
          next: status => this.cycleStatus.set(status),
          error: err => console.warn('Cycle status load failed', err)
        });
      }
    };

    if (this.userService.currentUser()) {
      loadOptional();
    } else {
      this.userService.getMe().subscribe({
        next: () => loadOptional(),
        error: err => console.warn('User load failed', err)
      });
    }

    this.dashboardService.getNewStravaActivity().subscribe({
      next: activity => {
        if (activity) {
          this.newStravaActivityDialog.open(activity);
        }
      },
      error: err => console.warn('New Strava activity check failed', err)
    });
  }

  // ── Optional second-row helpers ──────────────────────────────────
  asthmaEnabled(): boolean {
    return this.userService.currentUser()?.asthmaTrackingEnabled === true;
  }

  cycleEnabled(): boolean {
    return this.userService.currentUser()?.cycleTrackingEnabled === true;
  }

  showSecondRow(): boolean {
    return this.asthmaEnabled() || this.cycleEnabled();
  }

  asthmaRiskValue(): number | null {
    return this.bioWeather()?.asthmaRiskIndex ?? null;
  }

  asthmaRiskClass(): 'good' | 'warn' | 'bad' {
    const r = this.asthmaRiskValue() ?? 0;
    if (r < 30) return 'good';
    if (r < 60) return 'warn';
    return 'bad';
  }

  asthmaRiskLabel(): string {
    const r = this.asthmaRiskValue() ?? 0;
    if (r < 30) return 'DASHBOARD.ASTHMA_RISK_LOW';
    if (r < 60) return 'DASHBOARD.ASTHMA_RISK_MEDIUM';
    return 'DASHBOARD.ASTHMA_RISK_HIGH';
  }

  asthmaBarWidth(): number {
    const r = this.asthmaRiskValue();
    if (r == null) return 0;
    return Math.min(100, Math.max(0, r));
  }

  bloomingPollen(): Array<{ key: string; level: number; levelLabel: string }> {
    const bw = this.bioWeather();
    if (!bw) return [];
    const types: Array<{ key: string; value: number | null }> = [
      { key: 'DASHBOARD.POLLEN_BIRCH',   value: bw.pollenBirch },
      { key: 'DASHBOARD.POLLEN_GRASSES', value: bw.pollenGrasses },
      { key: 'DASHBOARD.POLLEN_MUGWORT', value: bw.pollenMugwort },
      { key: 'DASHBOARD.POLLEN_RAGWEED', value: bw.pollenRagweed },
      { key: 'DASHBOARD.POLLEN_HAZEL',   value: bw.pollenHazel },
      { key: 'DASHBOARD.POLLEN_ALDER',   value: bw.pollenAlder },
      { key: 'DASHBOARD.POLLEN_ASH',     value: bw.pollenAsh },
    ];
    const labelMap: Record<number, string> = {
      1: 'DASHBOARD.POLLEN_LEVEL_LOW',
      2: 'DASHBOARD.POLLEN_LEVEL_MEDIUM',
      3: 'DASHBOARD.POLLEN_LEVEL_HIGH'
    };
    return types
      .filter(t => (t.value ?? 0) > 0)
      .sort((a, b) => (b.value ?? 0) - (a.value ?? 0))
      .map(t => ({
        key: t.key,
        level: t.value as number,
        levelLabel: labelMap[Math.min(3, t.value as number)] ?? 'DASHBOARD.POLLEN_LEVEL_LOW'
      }));
  }

  cyclePhaseLabel(): string {
    const phase = this.cycleStatus()?.currentPhase ?? '';
    return `DASHBOARD.CYCLE_PHASE_${phase.toUpperCase()}`;
  }

  cyclePhasePerformanceHint(): string {
    const phase = (this.cycleStatus()?.currentPhase ?? '').toUpperCase();
    return `DASHBOARD.CYCLE_HINT_${phase}`;
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

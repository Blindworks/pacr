import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { finalize } from 'rxjs';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { MatIconModule } from '@angular/material/icon';
import { DashboardDto } from '../../models/dashboard.model';
import { ApiService } from '../../services/api.service';
import { ScoreRingState } from '../score-ring-card/score-ring-card.component';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatIconModule, TranslatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('loadTrendCanvas') loadTrendCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('efTrendCanvas') efTrendCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('driftTrendCanvas') driftTrendCanvas?: ElementRef<HTMLCanvasElement>;

  loading = true;
  errorMessage = '';
  dashboard?: DashboardDto;
  infoOpen: Record<string, boolean> = {};

  private loadTrendChart?: Chart;
  private efTrendChart?: Chart;
  private driftTrendChart?: Chart;
  private viewReady = false;

  constructor(
    private readonly apiService: ApiService,
    private readonly i18nService: I18nService
  ) {}

  ngOnInit(): void {
    this.fetchDashboard();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderCharts();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  get strainState(): ScoreRingState {
    const strain = this.dashboard?.strain21 ?? 0;

    if (strain < 9) {
      return 'good';
    }
    if (strain < 14) {
      return 'warn';
    }

    return 'bad';
  }

  get readinessState(): ScoreRingState {
    const score = this.dashboard?.readinessScore ?? 0;

    if (score >= 70) {
      return 'good';
    }
    if (score >= 45) {
      return 'warn';
    }

    return 'bad';
  }

  get loadState(): ScoreRingState {
    switch (this.dashboard?.loadStatus.flag) {
      case 'GREEN':
        return 'good';
      case 'ORANGE':
        return 'warn';
      case 'RED':
        return 'bad';
      case 'BLUE':
      default:
        return 'info';
    }
  }

  get loadSubtitle(): string {
    if (!this.dashboard) {
      return '';
    }

    return this.i18nService.t('dashboard.loadSubtitle', {
      flag: this.flagLabel(this.dashboard.loadStatus.flag),
      acwr: this.dashboard.loadStatus.acwr.toFixed(2)
    });
  }

  get loadFooterText(): string {
    if (!this.dashboard) {
      return '';
    }

    return this.i18nService.t('dashboard.loadFooter', { flag: this.flagLabel(this.dashboard.loadStatus.flag) });
  }

  get readinessSubtitle(): string {
    const recommendation = this.dashboard?.readinessRecommendation;
    if (!recommendation) {
      return '';
    }

    switch (recommendation.toUpperCase()) {
      case 'REST':
        return this.i18nService.t('dashboard.recommendationRest');
      case 'EASY':
        return this.i18nService.t('dashboard.recommendationEasy');
      case 'MODERATE':
        return this.i18nService.t('dashboard.recommendationModerate');
      case 'HARD':
        return this.i18nService.t('dashboard.recommendationHard');
      default:
        return this.i18nService.translateLiteral(recommendation);
    }
  }

  get recentWeek(): Array<{ label: string; shortLabel: string; heightPct: number }> {
    const trend = this.dashboard?.loadTrend ?? [];
    const last7 = trend.slice(-7);
    const maxVal = Math.max(...last7.map(p => p.strain21), 1);
    return last7.map(p => ({
      label: `${this.compactDate(p.date)}: ${p.strain21.toFixed(1)}`,
      shortLabel: new Date(p.date).toLocaleDateString(this.getDateLocale(), { weekday: 'narrow' }),
      heightPct: Math.max(5, (p.strain21 / maxVal) * 100)
    }));
  }

  get acwrDashOffset(): number {
    const acwr = this.dashboard?.loadStatus.acwr ?? 0;
    return 502 - Math.min(acwr / 2, 1) * 502;
  }

  get readinessCtaLabel(): string {
    const score = this.dashboard?.readinessScore ?? 0;
    if (score >= 70) return 'GO';
    if (score >= 45) return 'EASY';
    return 'REST';
  }

  get acwrFlagLabel(): string {
    return this.dashboard ? this.flagLabel(this.dashboard.loadStatus.flag) : '';
  }

  localizeBullet(bullet: string): string {
    return this.i18nService.translateLiteral(bullet);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString(this.getDateLocale(), {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  toggleInfo(key: string): void {
    this.infoOpen[key] = !this.infoOpen[key];
  }

  isInfoOpen(key: string): boolean {
    return !!this.infoOpen[key];
  }

  private fetchDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    this.apiService.getDashboard()
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: (dashboard) => {
          this.dashboard = dashboard;
          requestAnimationFrame(() => this.renderCharts());
        },
        error: (error) => {
          this.errorMessage = error?.error?.message || this.i18nService.t('dashboard.loadError');
          this.dashboard = undefined;
          this.destroyCharts();
        }
      });
  }

  private renderCharts(): void {
    if (!this.viewReady || !this.dashboard) {
      return;
    }

    this.createLoadTrendChart();
    this.createEfTrendChart();
    this.createDriftTrendChart();
  }

  private createLoadTrendChart(): void {
    const canvas = this.loadTrendCanvas?.nativeElement;
    if (!canvas) {
      return;
    }

    this.loadTrendChart?.destroy();

    const labels = this.dashboard?.loadTrend.map(point => this.compactDate(point.date)) ?? [];
    const values = this.dashboard?.loadTrend.map(point => point.strain21) ?? [];
    const accentBlue = this.themeColor('--accent-blue', '#2D7BFF');
    const textSecondary = this.themeColor('--text-secondary', '#9AA3B2');
    const border = this.themeColor('--border', '#1C2233');

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            data: values,
            borderColor: accentBlue,
            backgroundColor: this.withAlpha(accentBlue, 0.18),
            pointRadius: 0,
            pointHoverRadius: 4,
            borderWidth: 2,
            tension: 0.3,
            fill: true
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: {
          duration: 700,
          easing: 'easeOutQuart'
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: this.themeColor('--bg-panel', '#1A2235'),
            borderColor: border,
            borderWidth: 1,
            titleColor: '#EAF0FF',
            bodyColor: '#EAF0FF',
            displayColors: false,
            callbacks: {
              label: (context) => this.i18nService.t('dashboard.tooltipStrain', {
                value: Number(context.raw).toFixed(1)
              })
            }
          }
        },
        scales: {
          x: {
            ticks: {
              color: textSecondary,
              maxTicksLimit: 7
            },
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            suggestedMax: 21,
            ticks: {
              color: textSecondary,
              maxTicksLimit: 5
            },
            grid: {
              color: border
            }
          }
        }
      }
    };

    this.loadTrendChart = new Chart(canvas, config);
  }

  private createEfTrendChart(): void {
    const canvas = this.efTrendCanvas?.nativeElement;
    if (!canvas) {
      return;
    }

    this.efTrendChart?.destroy();

    const accent = this.themeColor('--volt-green', '#00FF9C');
    const labels = this.dashboard?.efTrend.map(point => this.compactDate(point.date)) ?? [];
    const values = this.dashboard?.efTrend.map(point => point.ef) ?? [];

    this.efTrendChart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          data: values,
          borderColor: accent,
          backgroundColor: this.withAlpha(accent, 0.2),
          borderWidth: 2,
          pointRadius: 0,
          tension: 0.3,
          fill: true
        }]
      },
      options: this.miniChartOptions(this.i18nService.t('dashboard.ef'))
    });
  }

  private createDriftTrendChart(): void {
    const canvas = this.driftTrendCanvas?.nativeElement;
    if (!canvas) {
      return;
    }

    this.driftTrendChart?.destroy();

    const accent = this.themeColor('--pulse-orange', '#FF6A00');
    const labels = this.dashboard?.driftTrend.map(point => this.compactDate(point.date)) ?? [];
    const values = this.dashboard?.driftTrend.map(point => point.driftPct) ?? [];

    this.driftTrendChart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          data: values,
          borderColor: accent,
          backgroundColor: this.withAlpha(accent, 0.18),
          borderWidth: 2,
          pointRadius: 0,
          tension: 0.3,
          fill: true
        }]
      },
      options: this.miniChartOptions(this.i18nService.t('dashboard.drift'))
    });
  }

  private miniChartOptions(metricLabel: string): ChartConfiguration<'line'>['options'] {
    const panelColor = this.themeColor('--bg-panel', '#1A2235');
    const border = this.themeColor('--border', '#1C2233');

    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: {
        duration: 650,
        easing: 'easeOutQuart'
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: panelColor,
          borderColor: border,
          borderWidth: 1,
          titleColor: '#EAF0FF',
          bodyColor: '#EAF0FF',
          displayColors: false,
          callbacks: {
            label: (context) => `${metricLabel}: ${Number(context.raw).toFixed(2)}`
          }
        }
      },
      scales: {
        x: {
          display: false,
          grid: {
            display: false
          }
        },
        y: {
          display: false,
          grid: {
            display: false
          }
        }
      }
    };
  }

  private destroyCharts(): void {
    this.loadTrendChart?.destroy();
    this.efTrendChart?.destroy();
    this.driftTrendChart?.destroy();
  }

  private compactDate(date: string): string {
    return new Date(date).toLocaleDateString(this.getDateLocale(), {
      month: 'short',
      day: 'numeric'
    });
  }

  private flagLabel(flag: DashboardDto['loadStatus']['flag']): string {
    switch (flag) {
      case 'GREEN':
        return this.i18nService.t('dashboard.flagGreen');
      case 'ORANGE':
        return this.i18nService.t('dashboard.flagOrange');
      case 'RED':
        return this.i18nService.t('dashboard.flagRed');
      case 'BLUE':
      default:
        return this.i18nService.t('dashboard.flagBlue');
    }
  }

  private getDateLocale(): string {
    return this.i18nService.getLanguage() === 'de' ? 'de-DE' : 'en-US';
  }

  private themeColor(varName: string, fallback: string): string {
    const value = getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
    return value || fallback;
  }

  private withAlpha(color: string, alpha: number): string {
    if (!color.startsWith('#')) {
      return color;
    }

    const hex = color.replace('#', '');
    const expanded = hex.length === 3
      ? hex.split('').map(char => `${char}${char}`).join('')
      : hex;

    const r = parseInt(expanded.slice(0, 2), 16);
    const g = parseInt(expanded.slice(2, 4), 16);
    const b = parseInt(expanded.slice(4, 6), 16);

    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}

import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { StatisticsService, TrainingStatsDto, StatsBucket } from '../../services/statistics.service';

type MonthlyBar = {
  month: string;
  heightPct: number;
  distanceKm: number;
};

type IntensityZone = {
  label: string;
  value: number;
  colorClass: string;
};

type Best = {
  label: string;
  value: string;
};

const PERIOD_MAP: Record<string, string> = {
  'Diese Woche':     'currentWeek',
  'Letzte Woche':    'lastWeek',
  'Dieser Monat':    'currentMonth',
  'Letzte 30 Tage':  'day',
  'Letzte 12 Wochen':'week',
  'Dieses Jahr':     'currentYear',
  'Letzte 12 Monate':'month',
  'Alle Zeit':       'all',
};

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './statistics.html',
  styleUrl: './statistics.scss'
})
export class Statistics implements OnInit {
  private readonly statisticsService = inject(StatisticsService);

  protected readonly periods = Object.keys(PERIOD_MAP);
  protected readonly selectedPeriodLabel = signal('Letzte Woche');

  protected stats: TrainingStatsDto | null = null;
  protected loading = false;

  protected monthlyBars: MonthlyBar[] = [];
  protected intensityZones: IntensityZone[] = [];

  protected readonly personalBests: Best[] = [
    { label: '5 Kilometers', value: '18:42' },
    { label: '10 Kilometers', value: '39:15' },
    { label: 'Half Marathon', value: '1:24:08' },
    { label: 'Full Marathon', value: '2:58:33' },
  ];

  ngOnInit(): void {
    this.loadStats();
  }

  protected setPeriod(label: string): void {
    this.selectedPeriodLabel.set(label);
    this.loadStats();
  }

  protected formatPace(secondsPerKm: number): string {
    if (!secondsPerKm || secondsPerKm <= 0) {
      return '--:--';
    }
    const minutes = Math.floor(secondsPerKm / 60);
    const seconds = Math.round(secondsPerKm % 60);
    return `${minutes}:${String(seconds).padStart(2, '0')}`;
  }

  protected formatDuration(totalSeconds: number): string {
    if (!totalSeconds || totalSeconds <= 0) {
      return '0h';
    }
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
  }

  private loadStats(): void {
    const period = PERIOD_MAP[this.selectedPeriodLabel()];
    this.loading = true;
    this.statisticsService.getStats(period).subscribe({
      next: (data) => {
        this.stats = data;
        this.monthlyBars = this.buildMonthlyBars(data.buckets);
        this.intensityZones = this.buildIntensityZones(data);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private buildMonthlyBars(buckets: StatsBucket[]): MonthlyBar[] {
    const last6 = buckets.slice(-6);
    const maxDistance = Math.max(...last6.map(b => b.distanceKm), 1);
    return last6.map(b => ({
      month: b.label,
      heightPct: Math.round((b.distanceKm / maxDistance) * 100),
      distanceKm: b.distanceKm,
    }));
  }

  private buildIntensityZones(data: TrainingStatsDto): IntensityZone[] {
    const z1 = data.totalZone1Seconds ?? 0;
    const z2 = data.totalZone2Seconds ?? 0;
    const z3 = data.totalZone3Seconds ?? 0;
    const z4 = data.totalZone4Seconds ?? 0;
    const z5 = data.totalZone5Seconds ?? 0;
    const total = z1 + z2 + z3 + z4 + z5;

    if (total === 0) {
      return [
        { label: 'Easy (Zone 1-2)', value: 0, colorClass: 'zone--easy' },
        { label: 'Tempo (Zone 3)', value: 0, colorClass: 'zone--tempo' },
        { label: 'Threshold (Zone 4)', value: 0, colorClass: 'zone--threshold' },
        { label: 'Anaerobic (Zone 5)', value: 0, colorClass: 'zone--anaerobic' },
      ];
    }

    const pct = (seconds: number) => Math.round((seconds / total) * 100);

    return [
      { label: 'Easy (Zone 1-2)', value: pct(z1 + z2), colorClass: 'zone--easy' },
      { label: 'Tempo (Zone 3)', value: pct(z3), colorClass: 'zone--tempo' },
      { label: 'Threshold (Zone 4)', value: pct(z4), colorClass: 'zone--threshold' },
      { label: 'Anaerobic (Zone 5)', value: pct(z5), colorClass: 'zone--anaerobic' },
    ];
  }
}

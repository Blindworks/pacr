import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { StatisticsService, TrainingStatsDto, StatsBucket, Vo2MaxPoint } from '../../services/statistics.service';

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

  protected fitnessTrendPath = '';
  protected fitnessTrendArea = '';
  protected fitnessTrendLabels: string[] = [];

  protected trendMarkerVisible = false;
  protected trendMarkerX = 0;       // 0–100 %, für CSS left
  protected trendMarkerSvgX = 0;    // 0–400, für SVG-Linie
  protected trendMarkerSvgY: number | null = null;
  protected trendMarkerValue = '';
  protected trendMarkerDate = '';

  private fitnessTrendDots: { x: number; y: number; vo2max: number; timestamp: number }[] = [];

  protected readonly personalBests: Best[] = [
    { label: '5 Kilometers', value: '18:42' },
    { label: '10 Kilometers', value: '39:15' },
    { label: 'Half Marathon', value: '1:24:08' },
    { label: 'Full Marathon', value: '2:58:33' },
  ];

  ngOnInit(): void {
    this.loadStats();
    this.statisticsService.getVo2MaxHistory().subscribe({
      next: data => this.buildFitnessTrend(data),
      error: () => {},
    });
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

  protected onTrendMouseMove(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const pct = ((event.clientX - rect.left) / rect.width) * 100;
    if (pct < 0 || pct > 100 || !this.fitnessTrendDots.length) {
      this.trendMarkerVisible = false;
      return;
    }
    this.trendMarkerX = pct;
    this.trendMarkerSvgX = pct * 4;
    this.trendMarkerVisible = true;
    this.trendMarkerSvgY = this.interpolateTrendY(this.trendMarkerSvgX);
    const rawVal = this.interpolateTrendValue(this.trendMarkerSvgX);
    this.trendMarkerValue = rawVal != null ? rawVal.toFixed(1) : '';
    const ts = this.interpolateTrendTimestamp(this.trendMarkerSvgX);
    this.trendMarkerDate = ts != null
      ? new Date(ts).toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' })
      : '';
  }

  protected onTrendMouseLeave(): void {
    this.trendMarkerVisible = false;
  }

  private interpolateTrendY(svgX: number): number | null {
    const pts = this.fitnessTrendDots;
    if (!pts.length) return null;
    if (svgX <= pts[0].x) return pts[0].y;
    if (svgX >= pts[pts.length - 1].x) return pts[pts.length - 1].y;
    for (let i = 1; i < pts.length; i++) {
      if (svgX <= pts[i].x) {
        const t = (svgX - pts[i - 1].x) / (pts[i].x - pts[i - 1].x);
        return pts[i - 1].y + (pts[i].y - pts[i - 1].y) * t;
      }
    }
    return pts[pts.length - 1].y;
  }

  private interpolateTrendValue(svgX: number): number | null {
    const pts = this.fitnessTrendDots;
    if (!pts.length) return null;
    if (svgX <= pts[0].x) return pts[0].vo2max;
    if (svgX >= pts[pts.length - 1].x) return pts[pts.length - 1].vo2max;
    for (let i = 1; i < pts.length; i++) {
      if (svgX <= pts[i].x) {
        const t = (svgX - pts[i - 1].x) / (pts[i].x - pts[i - 1].x);
        return pts[i - 1].vo2max + (pts[i].vo2max - pts[i - 1].vo2max) * t;
      }
    }
    return pts[pts.length - 1].vo2max;
  }

  private interpolateTrendTimestamp(svgX: number): number | null {
    const pts = this.fitnessTrendDots;
    if (!pts.length) return null;
    if (svgX <= pts[0].x) return pts[0].timestamp;
    if (svgX >= pts[pts.length - 1].x) return pts[pts.length - 1].timestamp;
    for (let i = 1; i < pts.length; i++) {
      if (svgX <= pts[i].x) {
        const t = (svgX - pts[i - 1].x) / (pts[i].x - pts[i - 1].x);
        return pts[i - 1].timestamp + (pts[i].timestamp - pts[i - 1].timestamp) * t;
      }
    }
    return pts[pts.length - 1].timestamp;
  }

  private buildFitnessTrend(points: Vo2MaxPoint[]): void {
    if (points.length <= 1) {
      this.fitnessTrendPath = '';
      this.fitnessTrendArea = '';
      this.fitnessTrendLabels = [];
      return;
    }

    const W = 400;
    const H = 150;
    const PAD_TOP = 10;
    const PAD_BOT = 10;
    const yRange = H - PAD_TOP - PAD_BOT;

    const minV = Math.min(...points.map(p => p.vo2max));
    const maxV = Math.max(...points.map(p => p.vo2max));
    const vSpan = maxV - minV || 1;

    const coords: [number, number][] = points.map((p, i) => {
      const x = (i / (points.length - 1)) * W;
      const y = PAD_TOP + (1 - (p.vo2max - minV) / vSpan) * yRange;
      return [x, y];
    });

    // Catmull-Rom → Cubic Bézier
    const tension = 0.5;
    let linePath = `M${coords[0][0]},${coords[0][1]}`;
    for (let i = 0; i < coords.length - 1; i++) {
      const p0 = coords[Math.max(i - 1, 0)];
      const p1 = coords[i];
      const p2 = coords[i + 1];
      const p3 = coords[Math.min(i + 2, coords.length - 1)];
      const cp1x = p1[0] + (p2[0] - p0[0]) * tension / 3;
      const cp1y = p1[1] + (p2[1] - p0[1]) * tension / 3;
      const cp2x = p2[0] - (p3[0] - p1[0]) * tension / 3;
      const cp2y = p2[1] - (p3[1] - p1[1]) * tension / 3;
      linePath += ` C${cp1x},${cp1y} ${cp2x},${cp2y} ${p2[0]},${p2[1]}`;
    }

    this.fitnessTrendPath = linePath;
    this.fitnessTrendArea = `${linePath} L${W},${H} L0,${H} Z`;
    this.fitnessTrendDots = coords.map((c, i) => ({
      x: c[0],
      y: c[1],
      vo2max: points[i].vo2max,
      timestamp: new Date(points[i].date).getTime(),
    }));

    // Labels: max 6, gleichmäßig verteilt
    const MONTH_NAMES = ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'];
    const labelCount = Math.min(6, points.length);
    this.fitnessTrendLabels = Array.from({ length: labelCount }, (_, i) => {
      const idx = Math.round(i * (points.length - 1) / (labelCount - 1));
      const d = new Date(points[idx].date);
      return `${MONTH_NAMES[d.getMonth()]} ${d.getFullYear()}`;
    });
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

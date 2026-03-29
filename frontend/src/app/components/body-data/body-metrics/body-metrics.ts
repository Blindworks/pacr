import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { BloodPressureEntry, BloodPressureService } from '../../../services/blood-pressure.service';
import { BodyMeasurementEntry, BodyMeasurementService } from '../../../services/body-measurement.service';

interface SummaryCard {
  label: string;
  value: string;
  unit: string;
  change: string;
  trend: 'up' | 'down' | 'flat';
}

interface DetailMetric {
  icon: string;
  label: string;
  value: string;
  change: string;
  trend: 'up' | 'down' | 'flat';
}

interface HistoryItem {
  measurement: BodyMeasurementEntry;
  date: string;
  time: string;
  weight: string;
  bodyFat: string;
}

interface StreamDescriptor {
  key: string;
  label: string;
  color: string;
  unit: string;
}

@Component({
  selector: 'app-body-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './body-metrics.html',
  styleUrl: './body-metrics.scss'
})
export class BodyMetrics implements OnInit {
  search = '';
  lastUpdatedText = 'No data available';
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly bodyMeasurementService = inject(BodyMeasurementService);
  private readonly bloodPressureService = inject(BloodPressureService);
  private bloodPressures: BloodPressureEntry[] = [];

  summaryCards: SummaryCard[] = [
    { label: 'Weight', value: '-', unit: 'kg', change: 'Stable', trend: 'flat' },
    { label: 'Body Fat', value: '-', unit: '%', change: 'Stable', trend: 'flat' },
    { label: 'Muscle Mass', value: '-', unit: 'kg', change: 'Stable', trend: 'flat' },
    { label: 'BMI', value: '-', unit: '', change: 'Stable', trend: 'flat' },
  ];

  detailMetrics: DetailMetric[] = [
    { icon: 'straighten', label: 'Waist Circ.', value: 'No data', change: 'Stable', trend: 'flat' },
    { icon: 'favorite', label: 'Resting HR', value: 'No data', change: 'Stable', trend: 'flat' },
    { icon: 'blood_pressure', label: 'Blood Pressure', value: 'No data', change: 'Stable', trend: 'flat' },
    { icon: 'water_drop', label: 'Water %', value: 'No data', change: 'Stable', trend: 'flat' },
    { icon: 'skeleton', label: 'Bone Mass', value: 'No data', change: 'Stable', trend: 'flat' },
    { icon: 'monitor_weight', label: 'Visceral Fat', value: 'No data', change: 'Stable', trend: 'flat' },
  ];

  historyItems: HistoryItem[] = [];

  // ── Chart: Stream config (like activity-detail) ──────────────────
  readonly streamDescriptors: StreamDescriptor[] = [
    { key: 'weight', label: 'Weight', color: '#b9f20d', unit: 'kg' },
    { key: 'fat', label: 'Body Fat', color: 'rgba(255,255,255,0.5)', unit: '%' },
    { key: 'muscle', label: 'Muscle', color: '#60a5fa', unit: 'kg' },
  ];

  activeStreams = signal<Set<string>>(new Set(['weight', 'fat', 'muscle']));

  readonly plotW = 880;
  readonly plotH = 250;

  hasChartData = false;
  tooltipIndex: number | null = null;
  tooltipPixelX: number | null = null;

  // Raw stream data arrays (parallel: same length, indexed by sorted measurement)
  private streamData: Record<string, (number | null)[]> = {};
  private timestamps: number[] = [];

  ngOnInit(): void {
    this.loadData();
  }

  goToLogNewData(): void {
    this.router.navigate(['/body-data/log-body-metrics']);
  }

  toggleStream(key: string): void {
    const next = new Set(this.activeStreams());
    if (next.has(key)) next.delete(key); else next.add(key);
    this.activeStreams.set(next);
  }

  private loadData(): void {
    forkJoin({
      latestMeasurement: this.bodyMeasurementService.getLatest().pipe(catchError(() => of(null))),
      measurements: this.bodyMeasurementService.getAll().pipe(catchError(() => of([]))),
      bloodPressures: this.bloodPressureService.getAll().pipe(catchError(() => of([]))),
      latestBloodPressure: this.bloodPressureService.getLatest().pipe(catchError(() => of(null)))
    }).subscribe(({ latestMeasurement, measurements, bloodPressures, latestBloodPressure }) => {
      const effectiveLatestMeasurement = latestMeasurement ?? measurements[0] ?? null;
      this.bloodPressures = bloodPressures;

      const previousMeasurement = measurements[1] ?? null;
      this.summaryCards = this.buildSummaryCards(effectiveLatestMeasurement, previousMeasurement);
      this.detailMetrics = this.buildDetailMetrics(
        effectiveLatestMeasurement,
        this.resolveBloodPressureForMeasurement(effectiveLatestMeasurement, latestBloodPressure)
      );
      this.historyItems = this.buildHistoryItems(measurements);
      this.lastUpdatedText = this.resolveLastUpdated(
        effectiveLatestMeasurement,
        this.resolveBloodPressureForMeasurement(effectiveLatestMeasurement, latestBloodPressure)
      );
      this.buildChartData(measurements);
      this.cdr.detectChanges();
    });
  }

  loadHistoryItem(item: HistoryItem): void {
    const matchingBloodPressure = this.resolveBloodPressureForMeasurement(item.measurement, null);
    const idx = this.historyItems.indexOf(item);
    const prev = idx >= 0 && idx + 1 < this.historyItems.length ? this.historyItems[idx + 1].measurement : null;
    this.summaryCards = this.buildSummaryCards(item.measurement, prev);
    this.detailMetrics = this.buildDetailMetrics(item.measurement, matchingBloodPressure);
    this.lastUpdatedText = this.resolveLastUpdated(item.measurement, matchingBloodPressure);
    this.cdr.detectChanges();
  }

  // ── Chart: computed getters ──────────────────────────────────────

  /** Streams that have data (for legend). */
  get availableStreams(): StreamDescriptor[] {
    return this.streamDescriptors.filter(d => {
      const data = this.streamData[d.key];
      return data && data.some(v => v !== null);
    });
  }

  /** Streams that have data AND are toggled on. */
  get availableActiveStreams(): StreamDescriptor[] {
    const active = this.activeStreams();
    return this.availableStreams.filter(d => active.has(d.key));
  }

  getStreamData(key: string): (number | null)[] | null {
    return this.streamData[key] ?? null;
  }

  streamRange(data: (number | null)[]): { min: number; max: number } | null {
    const vals = data.filter((v): v is number => v !== null);
    if (vals.length === 0) return null;
    return { min: Math.min(...vals), max: Math.max(...vals) };
  }

  /** Builds SVG path string for one stream using a smooth cardinal spline. */
  buildStreamPath(data: (number | null)[], range: { min: number; max: number }): string {
    const span = range.max - range.min || 1;
    const n = data.length;
    if (n === 0) return '';

    const pts: { x: number; y: number }[] = [];
    for (let i = 0; i < n; i++) {
      const v = data[i];
      if (v === null || v === undefined) continue;
      const x = n === 1 ? this.plotW / 2 : (i / (n - 1)) * this.plotW;
      const norm = (v - range.min) / span;
      pts.push({ x, y: this.plotH - norm * this.plotH });
    }

    if (pts.length < 2) return '';

    // Cardinal spline with tension 0.3
    const t = 0.3;
    let d = `M${pts[0].x.toFixed(1)},${pts[0].y.toFixed(1)}`;
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[Math.max(0, i - 1)];
      const p1 = pts[i];
      const p2 = pts[i + 1];
      const p3 = pts[Math.min(pts.length - 1, i + 2)];
      const cp1x = p1.x + (p2.x - p0.x) * t;
      const cp1y = p1.y + (p2.y - p0.y) * t;
      const cp2x = p2.x - (p3.x - p1.x) * t;
      const cp2y = p2.y - (p3.y - p1.y) * t;
      d += ` C${cp1x.toFixed(1)},${cp1y.toFixed(1)} ${cp2x.toFixed(1)},${cp2y.toFixed(1)} ${p2.x.toFixed(1)},${p2.y.toFixed(1)}`;
    }
    return d;
  }

  get yAxisItems(): { color: string; ticks: { y: number; label: string }[] }[] {
    return this.availableActiveStreams.map(d => {
      const data = this.getStreamData(d.key);
      if (!data) return null;
      const range = this.streamRange(data);
      if (!range) return null;
      const ticks = [0, 0.5, 1].map(frac => {
        const val = range.min + frac * (range.max - range.min);
        const y = this.plotH - frac * this.plotH;
        return { y, label: this.formatNumber(val) };
      });
      return { color: d.color, ticks };
    }).filter(Boolean) as { color: string; ticks: { y: number; label: string }[] }[];
  }

  get xAxisTicks(): { label: string; x: number }[] {
    if (this.timestamps.length === 0) return [];
    const n = this.timestamps.length;
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const ticks: { label: string; x: number }[] = [];
    const count = Math.min(n, 6);
    for (let i = 0; i < count; i++) {
      const idx = count === 1 ? 0 : Math.round(i * (n - 1) / (count - 1));
      const d = new Date(this.timestamps[idx]);
      const x = n === 1 ? this.plotW / 2 : (idx / (n - 1)) * this.plotW;
      ticks.push({ label: `${d.getDate()} ${months[d.getMonth()]}`, x });
    }
    return ticks;
  }

  onChartMouseMove(event: MouseEvent): void {
    const rect = (event.currentTarget as SVGElement).getBoundingClientRect();
    const relX = event.clientX - rect.left;
    const n = this.timestamps.length;
    if (n === 0) return;

    const targetIdx = (relX / rect.width) * (n - 1);
    let closest = 0;
    let minDiff = Math.abs(0 - targetIdx);
    for (let i = 1; i < n; i++) {
      const diff = Math.abs(i - targetIdx);
      if (diff < minDiff) { minDiff = diff; closest = i; }
    }
    this.tooltipIndex = closest;
    this.tooltipPixelX = relX;
    this.cdr.detectChanges();
  }

  onChartMouseLeave(): void {
    this.tooltipIndex = null;
    this.tooltipPixelX = null;
  }

  hoverX(idx: number): number {
    const n = this.timestamps.length;
    if (n <= 1) return this.plotW / 2;
    return (idx / (n - 1)) * this.plotW;
  }

  hoverDotY(key: string): number | null {
    if (this.tooltipIndex === null) return null;
    const data = this.getStreamData(key);
    if (!data) return null;
    const v = data[this.tooltipIndex];
    if (v === null || v === undefined) return null;
    const range = this.streamRange(data);
    if (!range) return null;
    const span = range.max - range.min || 1;
    const norm = (v - range.min) / span;
    return this.plotH - norm * this.plotH;
  }

  get tooltipDate(): string {
    if (this.tooltipIndex === null || this.timestamps.length === 0) return '';
    const ts = this.timestamps[this.tooltipIndex];
    return new Date(ts).toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  tooltipValue(key: string): string | null {
    if (this.tooltipIndex === null) return null;
    const data = this.getStreamData(key);
    if (!data) return null;
    const v = data[this.tooltipIndex];
    if (v === null || v === undefined) return null;
    return this.formatNumber(v);
  }

  // ── Chart data build ───────────────────────────────────────────

  private buildChartData(measurements: BodyMeasurementEntry[]): void {
    const sorted = [...measurements]
      .filter(m => m.weightKg != null || m.fatPercentage != null || m.muscleMassKg != null)
      .sort((a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime());

    this.hasChartData = sorted.length > 0;

    if (sorted.length === 0) {
      this.streamData = {};
      this.timestamps = [];
      return;
    }

    this.timestamps = sorted.map(m => new Date(m.measuredAt).getTime());
    this.streamData = {
      weight: sorted.map(m => m.weightKg ?? null),
      fat: sorted.map(m => m.fatPercentage ?? null),
      muscle: sorted.map(m => m.muscleMassKg ?? null),
    };
  }

  private buildSummaryCards(latest: BodyMeasurementEntry | null, previous: BodyMeasurementEntry | null): SummaryCard[] {
    return [
      this.createSummaryCard('Weight', latest?.weightKg, 'kg', previous?.weightKg),
      this.createSummaryCard('Body Fat', latest?.fatPercentage, '%', previous?.fatPercentage),
      this.createSummaryCard('Muscle Mass', latest?.muscleMassKg, 'kg', previous?.muscleMassKg),
      this.createSummaryCard('BMI', latest?.bmi, '', previous?.bmi),
    ];
  }

  private buildDetailMetrics(
    latestMeasurement: BodyMeasurementEntry | null,
    latestBloodPressure: BloodPressureEntry | null
  ): DetailMetric[] {
    return [
      { icon: 'straighten', label: 'Waist Circ.', value: 'No data', change: 'Stable', trend: 'flat' },
      {
        icon: 'favorite',
        label: 'Resting HR',
        value: latestBloodPressure?.pulseAtMeasurement != null ? `${latestBloodPressure.pulseAtMeasurement} bpm` : 'No data',
        change: 'Stable',
        trend: 'flat'
      },
      {
        icon: 'blood_pressure',
        label: 'Blood Pressure',
        value: latestBloodPressure ? `${latestBloodPressure.systolicPressure}/${latestBloodPressure.diastolicPressure}` : 'No data',
        change: 'Stable',
        trend: 'flat'
      },
      {
        icon: 'water_drop',
        label: 'Water %',
        value: latestMeasurement?.waterPercentage != null ? `${this.formatNumber(latestMeasurement.waterPercentage)}%` : 'No data',
        change: 'Stable',
        trend: 'flat'
      },
      {
        icon: 'skeleton',
        label: 'Bone Mass',
        value: latestMeasurement?.boneMassKg != null ? `${this.formatNumber(latestMeasurement.boneMassKg)} kg` : 'No data',
        change: 'Stable',
        trend: 'flat'
      },
      {
        icon: 'monitor_weight',
        label: 'Visceral Fat',
        value: latestMeasurement?.visceralFatLevel != null ? `Level ${latestMeasurement.visceralFatLevel}` : 'No data',
        change: 'Stable',
        trend: 'flat'
      },
    ];
  }

  private buildHistoryItems(measurements: BodyMeasurementEntry[]): HistoryItem[] {
    const term = this.search.trim().toLowerCase();
    return measurements
      .filter(item => {
        if (!term) {
          return true;
        }

        const searchable = `${item.measuredAt} ${item.weightKg ?? ''} ${item.fatPercentage ?? ''}`.toLowerCase();
        return searchable.includes(term);
      })
      .slice(0, 4)
      .map(item => ({
        measurement: item,
        date: this.formatLongDate(item.measuredAt),
        time: '08:00 AM',
        weight: item.weightKg != null ? `${this.formatNumber(item.weightKg)} kg` : 'No data',
        bodyFat: item.fatPercentage != null ? `${this.formatNumber(item.fatPercentage)}%` : 'No data'
      }));
  }

  private resolveBloodPressureForMeasurement(
    measurement: BodyMeasurementEntry | null,
    fallback: BloodPressureEntry | null
  ): BloodPressureEntry | null {
    if (!measurement) {
      return fallback;
    }

    return this.bloodPressures.find(entry => entry.measuredAt === measurement.measuredAt) ?? fallback;
  }

  private resolveLastUpdated(
    latestMeasurement: BodyMeasurementEntry | null,
    latestBloodPressure: BloodPressureEntry | null
  ): string {
    const dates = [
      latestMeasurement?.measuredAt,
      latestBloodPressure?.measuredAt
    ].filter((value): value is string => !!value);

    if (dates.length === 0) {
      return 'No data available';
    }

    const latest = dates.sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0];
    return `${this.formatLongDate(latest)}, 08:00 AM`;
  }

  private createSummaryCard(label: string, value: number | undefined, unit = '', previous?: number): SummaryCard {
    let change = 'No prev.';
    let trend: 'up' | 'down' | 'flat' = 'flat';

    if (value != null && previous != null) {
      const delta = value - previous;
      const sign = delta > 0 ? '+' : '';
      change = `${sign}${this.formatNumber(delta)}${unit ? ' ' + unit : ''}`;
      if (Math.abs(delta) < 0.05) {
        change = 'Stable';
        trend = 'flat';
      } else {
        trend = delta > 0 ? 'up' : 'down';
      }
    }

    return {
      label,
      value: value != null ? this.formatNumber(value) : '-',
      unit,
      change,
      trend
    };
  }

  private formatNumber(value: number, fractionDigits = 1): string {
    return value.toLocaleString('de-DE', {
      minimumFractionDigits: 0,
      maximumFractionDigits: fractionDigits
    });
  }

  private formatLongDate(value: string): string {
    return new Date(value).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }
}

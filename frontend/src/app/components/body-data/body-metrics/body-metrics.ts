import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BloodPressureEntry, BloodPressureService } from '../../../services/blood-pressure.service';
import { ProOverlay } from '../../shared/pro-overlay/pro-overlay';
import { BodyMeasurementEntry, BodyMeasurementService } from '../../../services/body-measurement.service';
import { SleepDataEntry, SleepDataService } from '../../../services/sleep-data.service';

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

type HistoryEntryType = 'body' | 'bloodPressure' | 'restingHr';

interface HistoryItem {
  id: number;
  type: HistoryEntryType;
  date: string;
  rawDate: string;
  icon: string;
  label: string;
  values: { label: string; value: string }[];
  originalEntry: BodyMeasurementEntry | BloodPressureEntry | SleepDataEntry;
  editing: boolean;
  editFields: Record<string, number | string | null>;
}

interface StreamDescriptor {
  key: string;
  label: string;
  color: string;
  unit: string;
}

interface ChartContext {
  timestamps: number[];
  streamData: Record<string, (number | null)[]>;
  descriptors: StreamDescriptor[];
  activeStreams: ReturnType<typeof signal<Set<string>>>;
  tooltipIndex: number | null;
  tooltipPixelX: number | null;
  hasData: boolean;
}

@Component({
  selector: 'app-body-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule, ProOverlay, TranslateModule],
  templateUrl: './body-metrics.html',
  styleUrl: './body-metrics.scss'
})
export class BodyMetrics implements OnInit {
  search = '';
  lastUpdatedText = '';
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly translate = inject(TranslateService);
  private readonly bodyMeasurementService = inject(BodyMeasurementService);
  private readonly bloodPressureService = inject(BloodPressureService);
  private readonly sleepDataService = inject(SleepDataService);
  private bloodPressures: BloodPressureEntry[] = [];
  private sleepDataEntries: SleepDataEntry[] = [];

  summaryCards: SummaryCard[] = [
    { label: 'BODY_DATA.WEIGHT', value: '-', unit: 'kg', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { label: 'BODY_DATA.BODY_FAT', value: '-', unit: '%', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { label: 'BODY_DATA.MUSCLE_MASS', value: '-', unit: 'kg', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { label: 'BODY_DATA.BMI', value: '-', unit: '', change: 'BODY_DATA.STABLE', trend: 'flat' },
  ];

  detailMetrics: DetailMetric[] = [
    { icon: 'straighten', label: 'BODY_DATA.WAIST', value: 'BODY_DATA.NO_DATA', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { icon: 'favorite', label: 'BODY_DATA.RESTING_HR', value: 'BODY_DATA.NO_DATA', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { icon: 'blood_pressure', label: 'BODY_DATA.BLOOD_PRESSURE', value: 'BODY_DATA.NO_DATA', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { icon: 'water_drop', label: 'BODY_DATA.WATER', value: 'BODY_DATA.NO_DATA', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { icon: 'skeleton', label: 'BODY_DATA.BONE_MASS', value: 'BODY_DATA.NO_DATA', change: 'BODY_DATA.STABLE', trend: 'flat' },
    { icon: 'monitor_weight', label: 'BODY_DATA.VISCERAL_FAT', value: 'BODY_DATA.NO_DATA', change: 'BODY_DATA.STABLE', trend: 'flat' },
  ];

  historyItems: HistoryItem[] = [];

  readonly plotW = 880;
  readonly plotH = 250;

  // ── Chart contexts ─────────────────────────────────────────────
  bodyChart: ChartContext = {
    timestamps: [],
    streamData: {},
    descriptors: [
      { key: 'weight', label: this.translate.instant('BODY_DATA.WEIGHT'), color: 'var(--pp)', unit: 'kg' },
      { key: 'fat', label: this.translate.instant('BODY_DATA.BODY_FAT'), color: 'rgba(255,255,255,0.5)', unit: '%' },
      { key: 'muscle', label: this.translate.instant('BODY_DATA.MUSCLE_MASS'), color: '#60a5fa', unit: 'kg' },
    ],
    activeStreams: signal<Set<string>>(new Set(['weight', 'fat', 'muscle'])),
    tooltipIndex: null,
    tooltipPixelX: null,
    hasData: false,
  };

  bpChart: ChartContext = {
    timestamps: [],
    streamData: {},
    descriptors: [
      { key: 'systolic', label: this.translate.instant('BODY_DATA.SYSTOLIC'), color: '#f87171', unit: 'mmHg' },
      { key: 'diastolic', label: this.translate.instant('BODY_DATA.DIASTOLIC'), color: '#60a5fa', unit: 'mmHg' },
    ],
    activeStreams: signal<Set<string>>(new Set(['systolic', 'diastolic'])),
    tooltipIndex: null,
    tooltipPixelX: null,
    hasData: false,
  };

  hrChart: ChartContext = {
    timestamps: [],
    streamData: {},
    descriptors: [
      { key: 'restingHr', label: this.translate.instant('BODY_DATA.RESTING_HR'), color: '#f472b6', unit: 'bpm' },
    ],
    activeStreams: signal<Set<string>>(new Set(['restingHr'])),
    tooltipIndex: null,
    tooltipPixelX: null,
    hasData: false,
  };

  // Keep legacy getters for template backward-compat
  get hasChartData(): boolean { return this.bodyChart.hasData; }

  ngOnInit(): void {
    this.loadData();
  }

  goToLogNewData(): void {
    this.router.navigate(['/body-data/log-body-metrics']);
  }

  // ── Generic chart helpers ──────────────────────────────────────

  toggleStream(ctx: ChartContext, key: string): void {
    const next = new Set(ctx.activeStreams());
    if (next.has(key)) next.delete(key); else next.add(key);
    ctx.activeStreams.set(next);
  }

  availableStreams(ctx: ChartContext): StreamDescriptor[] {
    return ctx.descriptors.filter(d => {
      const data = ctx.streamData[d.key];
      return data && data.some(v => v !== null);
    });
  }

  availableActiveStreams(ctx: ChartContext): StreamDescriptor[] {
    const active = ctx.activeStreams();
    return this.availableStreams(ctx).filter(d => active.has(d.key));
  }

  getStreamData(ctx: ChartContext, key: string): (number | null)[] | null {
    return ctx.streamData[key] ?? null;
  }

  streamRange(data: (number | null)[]): { min: number; max: number } | null {
    const vals = data.filter((v): v is number => v !== null);
    if (vals.length === 0) return null;
    return { min: Math.min(...vals), max: Math.max(...vals) };
  }

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

  yAxisItems(ctx: ChartContext): { color: string; ticks: { y: number; label: string }[] }[] {
    return this.availableActiveStreams(ctx).map(d => {
      const data = this.getStreamData(ctx, d.key);
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

  xAxisTicks(ctx: ChartContext): { label: string; x: number }[] {
    if (ctx.timestamps.length === 0) return [];
    const n = ctx.timestamps.length;
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const ticks: { label: string; x: number }[] = [];
    const count = Math.min(n, 6);
    for (let i = 0; i < count; i++) {
      const idx = count === 1 ? 0 : Math.round(i * (n - 1) / (count - 1));
      const d = new Date(ctx.timestamps[idx]);
      const x = n === 1 ? this.plotW / 2 : (idx / (n - 1)) * this.plotW;
      ticks.push({ label: `${d.getDate()} ${months[d.getMonth()]}`, x });
    }
    return ticks;
  }

  onChartMouseMove(ctx: ChartContext, event: MouseEvent): void {
    const rect = (event.currentTarget as SVGElement).getBoundingClientRect();
    const relX = event.clientX - rect.left;
    const n = ctx.timestamps.length;
    if (n === 0) return;

    const targetIdx = (relX / rect.width) * (n - 1);
    let closest = 0;
    let minDiff = Math.abs(0 - targetIdx);
    for (let i = 1; i < n; i++) {
      const diff = Math.abs(i - targetIdx);
      if (diff < minDiff) { minDiff = diff; closest = i; }
    }
    ctx.tooltipIndex = closest;
    ctx.tooltipPixelX = relX;
    this.cdr.detectChanges();
  }

  onChartMouseLeave(ctx: ChartContext): void {
    ctx.tooltipIndex = null;
    ctx.tooltipPixelX = null;
  }

  hoverX(ctx: ChartContext, idx: number): number {
    const n = ctx.timestamps.length;
    if (n <= 1) return this.plotW / 2;
    return (idx / (n - 1)) * this.plotW;
  }

  hoverDotY(ctx: ChartContext, key: string): number | null {
    if (ctx.tooltipIndex === null) return null;
    const data = this.getStreamData(ctx, key);
    if (!data) return null;
    const v = data[ctx.tooltipIndex];
    if (v === null || v === undefined) return null;
    const range = this.streamRange(data);
    if (!range) return null;
    const span = range.max - range.min || 1;
    const norm = (v - range.min) / span;
    return this.plotH - norm * this.plotH;
  }

  tooltipDate(ctx: ChartContext): string {
    if (ctx.tooltipIndex === null || ctx.timestamps.length === 0) return '';
    const ts = ctx.timestamps[ctx.tooltipIndex];
    return new Date(ts).toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  tooltipValue(ctx: ChartContext, key: string): string | null {
    if (ctx.tooltipIndex === null) return null;
    const data = this.getStreamData(ctx, key);
    if (!data) return null;
    const v = data[ctx.tooltipIndex];
    if (v === null || v === undefined) return null;
    return this.formatNumber(v);
  }

  // ── Data loading ───────────────────────────────────────────────

  private loadData(): void {
    forkJoin({
      latestMeasurement: this.bodyMeasurementService.getLatest().pipe(catchError(() => of(null))),
      measurements: this.bodyMeasurementService.getAll().pipe(catchError(() => of([]))),
      bloodPressures: this.bloodPressureService.getAll().pipe(catchError(() => of([]))),
      latestBloodPressure: this.bloodPressureService.getLatest().pipe(catchError(() => of(null))),
      sleepData: this.sleepDataService.getAll().pipe(catchError(() => of([])))
    }).subscribe(({ latestMeasurement, measurements, bloodPressures, latestBloodPressure, sleepData }) => {
      const effectiveLatestMeasurement = latestMeasurement ?? measurements[0] ?? null;
      this.bloodPressures = bloodPressures;
      this.sleepDataEntries = sleepData;

      const previousMeasurement = measurements[1] ?? null;
      this.summaryCards = this.buildSummaryCards(effectiveLatestMeasurement, previousMeasurement);
      this.detailMetrics = this.buildDetailMetrics(
        effectiveLatestMeasurement,
        this.resolveBloodPressureForMeasurement(effectiveLatestMeasurement, latestBloodPressure)
      );
      this.historyItems = this.buildHistoryItems(measurements, bloodPressures, sleepData);
      this.lastUpdatedText = this.resolveLastUpdated(
        effectiveLatestMeasurement,
        this.resolveBloodPressureForMeasurement(effectiveLatestMeasurement, latestBloodPressure)
      );
      this.buildBodyChartData(measurements);
      this.buildBpChartData(bloodPressures);
      this.buildHrChartData(bloodPressures);
      this.cdr.detectChanges();
    });
  }

  loadHistoryItem(item: HistoryItem): void {
    if (item.type !== 'body') return;
    const m = item.originalEntry as BodyMeasurementEntry;
    const matchingBloodPressure = this.resolveBloodPressureForMeasurement(m, null);
    const bodyItems = this.historyItems.filter(i => i.type === 'body');
    const idx = bodyItems.indexOf(item);
    const prev = idx >= 0 && idx + 1 < bodyItems.length ? (bodyItems[idx + 1].originalEntry as BodyMeasurementEntry) : null;
    this.summaryCards = this.buildSummaryCards(m, prev);
    this.detailMetrics = this.buildDetailMetrics(m, matchingBloodPressure);
    this.lastUpdatedText = this.resolveLastUpdated(m, matchingBloodPressure);
    this.cdr.detectChanges();
  }

  // ── Edit / Delete ─────────────────────────────────────────────

  startEdit(item: HistoryItem, event: Event): void {
    event.stopPropagation();
    item.editing = true;
    item.editFields = {};
    if (item.type === 'body') {
      const e = item.originalEntry as BodyMeasurementEntry;
      item.editFields = { weightKg: e.weightKg ?? null, fatPercentage: e.fatPercentage ?? null };
    } else if (item.type === 'bloodPressure') {
      const e = item.originalEntry as BloodPressureEntry;
      item.editFields = { systolicPressure: e.systolicPressure, diastolicPressure: e.diastolicPressure, pulseAtMeasurement: e.pulseAtMeasurement ?? null };
    } else {
      const e = item.originalEntry as SleepDataEntry;
      item.editFields = { restingHeartRate: e.restingHeartRate ?? null };
    }
  }

  cancelEdit(item: HistoryItem, event: Event): void {
    event.stopPropagation();
    item.editing = false;
  }

  saveEdit(item: HistoryItem, event: Event): void {
    event.stopPropagation();
    const f = item.editFields;
    if (item.type === 'body') {
      const entry = { ...item.originalEntry as BodyMeasurementEntry, weightKg: f['weightKg'] as number, fatPercentage: f['fatPercentage'] as number };
      this.bodyMeasurementService.update(item.id, entry).subscribe(() => { item.editing = false; this.loadData(); });
    } else if (item.type === 'bloodPressure') {
      const entry = { ...item.originalEntry as BloodPressureEntry, systolicPressure: f['systolicPressure'] as number, diastolicPressure: f['diastolicPressure'] as number, pulseAtMeasurement: f['pulseAtMeasurement'] as number };
      this.bloodPressureService.update(item.id, entry).subscribe(() => { item.editing = false; this.loadData(); });
    } else {
      const entry = { ...item.originalEntry as SleepDataEntry, restingHeartRate: f['restingHeartRate'] as number };
      this.sleepDataService.update(item.id, entry).subscribe(() => { item.editing = false; this.loadData(); });
    }
  }

  deleteItem(item: HistoryItem, event: Event): void {
    event.stopPropagation();
    const typeLabel = item.type === 'body' ? this.translate.instant('BODY_DATA.WEIGHT') : item.type === 'bloodPressure' ? this.translate.instant('BODY_DATA.BLOOD_PRESSURE') : this.translate.instant('BODY_DATA.RESTING_HR');
    if (!confirm(`${this.translate.instant('COMMON.DELETE')} ${typeLabel} ${item.date}?`)) return;

    if (item.type === 'body') {
      this.bodyMeasurementService.delete(item.id).subscribe(() => this.loadData());
    } else if (item.type === 'bloodPressure') {
      this.bloodPressureService.delete(item.id).subscribe(() => this.loadData());
    } else {
      this.sleepDataService.delete(item.id).subscribe(() => this.loadData());
    }
  }

  // ── Chart data builders ────────────────────────────────────────

  private buildBodyChartData(measurements: BodyMeasurementEntry[]): void {
    const sorted = [...measurements]
      .filter(m => m.weightKg != null || m.fatPercentage != null || m.muscleMassKg != null)
      .sort((a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime());

    this.bodyChart.hasData = sorted.length > 0;
    if (sorted.length === 0) {
      this.bodyChart.streamData = {};
      this.bodyChart.timestamps = [];
      return;
    }

    this.bodyChart.timestamps = sorted.map(m => new Date(m.measuredAt).getTime());
    this.bodyChart.streamData = {
      weight: sorted.map(m => m.weightKg ?? null),
      fat: sorted.map(m => m.fatPercentage ?? null),
      muscle: sorted.map(m => m.muscleMassKg ?? null),
    };
  }

  private buildBpChartData(bloodPressures: BloodPressureEntry[]): void {
    const sorted = [...bloodPressures]
      .filter(bp => bp.systolicPressure != null || bp.diastolicPressure != null)
      .sort((a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime());

    this.bpChart.hasData = sorted.length > 0;
    if (sorted.length === 0) {
      this.bpChart.streamData = {};
      this.bpChart.timestamps = [];
      return;
    }

    this.bpChart.timestamps = sorted.map(bp => new Date(bp.measuredAt).getTime());
    this.bpChart.streamData = {
      systolic: sorted.map(bp => bp.systolicPressure ?? null),
      diastolic: sorted.map(bp => bp.diastolicPressure ?? null),
    };
  }

  private buildHrChartData(bloodPressures: BloodPressureEntry[]): void {
    const sorted = [...bloodPressures]
      .filter(bp => bp.pulseAtMeasurement != null)
      .sort((a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime());

    this.hrChart.hasData = sorted.length > 0;
    if (sorted.length === 0) {
      this.hrChart.streamData = {};
      this.hrChart.timestamps = [];
      return;
    }

    this.hrChart.timestamps = sorted.map(bp => new Date(bp.measuredAt).getTime());
    this.hrChart.streamData = {
      restingHr: sorted.map(bp => bp.pulseAtMeasurement ?? null),
    };
  }

  // ── Summary / Detail builders ──────────────────────────────────

  private buildSummaryCards(latest: BodyMeasurementEntry | null, previous: BodyMeasurementEntry | null): SummaryCard[] {
    return [
      this.createSummaryCard('BODY_DATA.WEIGHT', latest?.weightKg, 'kg', previous?.weightKg),
      this.createSummaryCard('BODY_DATA.BODY_FAT', latest?.fatPercentage, '%', previous?.fatPercentage),
      this.createSummaryCard('BODY_DATA.MUSCLE_MASS', latest?.muscleMassKg, 'kg', previous?.muscleMassKg),
      this.createSummaryCard('BODY_DATA.BMI', latest?.bmi, '', previous?.bmi),
    ];
  }

  private buildDetailMetrics(
    latestMeasurement: BodyMeasurementEntry | null,
    latestBloodPressure: BloodPressureEntry | null
  ): DetailMetric[] {
    const noData = this.translate.instant('BODY_DATA.NO_DATA');
    const stable = this.translate.instant('BODY_DATA.STABLE');
    return [
      { icon: 'straighten', label: 'BODY_DATA.WAIST', value: noData, change: stable, trend: 'flat' },
      {
        icon: 'favorite',
        label: 'BODY_DATA.RESTING_HR',
        value: latestBloodPressure?.pulseAtMeasurement != null ? `${latestBloodPressure.pulseAtMeasurement} bpm` : noData,
        change: stable,
        trend: 'flat'
      },
      {
        icon: 'blood_pressure',
        label: 'BODY_DATA.BLOOD_PRESSURE',
        value: latestBloodPressure ? `${latestBloodPressure.systolicPressure}/${latestBloodPressure.diastolicPressure}` : noData,
        change: stable,
        trend: 'flat'
      },
      {
        icon: 'water_drop',
        label: 'BODY_DATA.WATER',
        value: latestMeasurement?.waterPercentage != null ? `${this.formatNumber(latestMeasurement.waterPercentage)}%` : noData,
        change: stable,
        trend: 'flat'
      },
      {
        icon: 'skeleton',
        label: 'BODY_DATA.BONE_MASS',
        value: latestMeasurement?.boneMassKg != null ? `${this.formatNumber(latestMeasurement.boneMassKg)} kg` : noData,
        change: stable,
        trend: 'flat'
      },
      {
        icon: 'monitor_weight',
        label: 'BODY_DATA.VISCERAL_FAT',
        value: latestMeasurement?.visceralFatLevel != null ? `Level ${latestMeasurement.visceralFatLevel}` : noData,
        change: stable,
        trend: 'flat'
      },
    ];
  }

  private buildHistoryItems(
    measurements: BodyMeasurementEntry[],
    bloodPressures: BloodPressureEntry[],
    sleepData: SleepDataEntry[]
  ): HistoryItem[] {
    const term = this.search.trim().toLowerCase();
    const items: HistoryItem[] = [];

    for (const m of measurements) {
      if (m.id == null) continue;
      items.push({
        id: m.id,
        type: 'body',
        date: this.formatLongDate(m.measuredAt),
        rawDate: m.measuredAt,
        icon: 'monitor_weight',
        label: this.translate.instant('BODY_DATA.WEIGHT'),
        values: [
          { label: this.translate.instant('BODY_DATA.WEIGHT'), value: m.weightKg != null ? `${this.formatNumber(m.weightKg)} kg` : '-' },
          { label: this.translate.instant('BODY_DATA.BODY_FAT'), value: m.fatPercentage != null ? `${this.formatNumber(m.fatPercentage)}%` : '-' },
        ],
        originalEntry: m,
        editing: false,
        editFields: {},
      });
    }

    for (const bp of bloodPressures) {
      if (bp.id == null) continue;
      items.push({
        id: bp.id,
        type: 'bloodPressure',
        date: this.formatLongDate(bp.measuredAt),
        rawDate: bp.measuredAt,
        icon: 'blood_pressure',
        label: this.translate.instant('BODY_DATA.BLOOD_PRESSURE'),
        values: [
          { label: this.translate.instant('BODY_DATA.BLOOD_PRESSURE'), value: `${bp.systolicPressure}/${bp.diastolicPressure}` },
          { label: this.translate.instant('BODY_DATA.PULSE'), value: bp.pulseAtMeasurement != null ? `${bp.pulseAtMeasurement} bpm` : '-' },
        ],
        originalEntry: bp,
        editing: false,
        editFields: {},
      });
    }

    for (const sd of sleepData) {
      if (sd.id == null || sd.restingHeartRate == null) continue;
      items.push({
        id: sd.id,
        type: 'restingHr',
        date: this.formatLongDate(sd.recordedAt),
        rawDate: sd.recordedAt,
        icon: 'favorite',
        label: this.translate.instant('BODY_DATA.RESTING_HR'),
        values: [
          { label: this.translate.instant('BODY_DATA.RESTING_HR'), value: `${sd.restingHeartRate} bpm` },
        ],
        originalEntry: sd,
        editing: false,
        editFields: {},
      });
    }

    return items
      .filter(item => {
        if (!term) return true;
        const searchable = `${item.rawDate} ${item.label} ${item.values.map(v => v.value).join(' ')}`.toLowerCase();
        return searchable.includes(term);
      })
      .sort((a, b) => new Date(b.rawDate).getTime() - new Date(a.rawDate).getTime())
      .slice(0, 8);
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
      return this.translate.instant('BODY_DATA.NO_DATA_AVAILABLE');
    }

    const latest = dates.sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0];
    return `${this.formatLongDate(latest)}, 08:00 AM`;
  }

  private createSummaryCard(label: string, value: number | undefined, unit = '', previous?: number): SummaryCard {
    let change = this.translate.instant('BODY_DATA.NO_DATA');
    let trend: 'up' | 'down' | 'flat' = 'flat';

    if (value != null && previous != null) {
      const delta = value - previous;
      const sign = delta > 0 ? '+' : '';
      change = `${sign}${this.formatNumber(delta)}${unit ? ' ' + unit : ''}`;
      if (Math.abs(delta) < 0.05) {
        change = 'BODY_DATA.STABLE';
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

  formatNumber(value: number, fractionDigits = 1): string {
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

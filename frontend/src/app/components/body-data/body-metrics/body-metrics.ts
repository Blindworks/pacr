import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
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

interface ChartPoint {
  x: number;
  y: number;
  rawValue: number;
  timestamp: number;
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

  // Legend toggle state
  visibleLines = { weight: true, fat: true, muscle: true };

  // Chart data
  weightPath = '';
  weightFillPath = '';
  fatPath = '';
  musclePath = '';
  axisLabels: string[] = ['', '', '', 'TODAY'];
  hasChartData = false;

  // Marker state
  markerVisible = false;
  markerX = 0;
  markerWeightY: number | null = null;
  markerFatY: number | null = null;
  markerMuscleY: number | null = null;
  markerWeightValue = '';
  markerFatValue = '';
  markerMuscleValue = '';
  markerDate = '';

  private chartWeightPoints: ChartPoint[] = [];
  private chartFatPoints: ChartPoint[] = [];
  private chartMusclePoints: ChartPoint[] = [];

  ngOnInit(): void {
    this.loadData();
  }

  goToLogNewData(): void {
    this.router.navigate(['/body-data/log-body-metrics']);
  }

  toggleLine(line: 'weight' | 'fat' | 'muscle'): void {
    this.visibleLines[line] = !this.visibleLines[line];
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

  onChartMouseMove(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const svgX = ((event.clientX - rect.left) / rect.width) * 100;

    if (svgX < 0 || svgX > 100) {
      this.markerVisible = false;
      return;
    }

    this.markerX = svgX;
    this.markerVisible = true;

    this.markerWeightY = this.visibleLines.weight ? this.interpolateY(this.chartWeightPoints, svgX) : null;
    this.markerFatY = this.visibleLines.fat ? this.interpolateY(this.chartFatPoints, svgX) : null;
    this.markerMuscleY = this.visibleLines.muscle ? this.interpolateY(this.chartMusclePoints, svgX) : null;

    const weightVal = this.visibleLines.weight ? this.interpolateRawValue(this.chartWeightPoints, svgX) : null;
    const fatVal = this.visibleLines.fat ? this.interpolateRawValue(this.chartFatPoints, svgX) : null;
    const muscleVal = this.visibleLines.muscle ? this.interpolateRawValue(this.chartMusclePoints, svgX) : null;

    this.markerWeightValue = weightVal != null ? `${this.formatNumber(weightVal)} kg` : '';
    this.markerFatValue = fatVal != null ? `${this.formatNumber(fatVal)}%` : '';
    this.markerMuscleValue = muscleVal != null ? `${this.formatNumber(muscleVal)} kg` : '';

    const refPoints = this.chartWeightPoints.length > 0 ? this.chartWeightPoints
      : this.chartFatPoints.length > 0 ? this.chartFatPoints
      : this.chartMusclePoints;
    const ts = this.interpolateTimestamp(refPoints, svgX);
    this.markerDate = ts != null ? new Date(ts).toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' }) : '';
  }

  onChartMouseLeave(): void {
    this.markerVisible = false;
  }

  private buildChartData(measurements: BodyMeasurementEntry[]): void {
    const sorted = [...measurements]
      .filter(m => m.weightKg != null || m.fatPercentage != null || m.muscleMassKg != null)
      .sort((a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime());

    this.hasChartData = sorted.length > 0;

    if (sorted.length === 0) {
      this.weightPath = '';
      this.weightFillPath = '';
      this.fatPath = '';
      this.musclePath = '';
      this.chartWeightPoints = [];
      this.chartFatPoints = [];
      this.chartMusclePoints = [];
      return;
    }

    const timestamps = sorted.map(m => new Date(m.measuredAt).getTime());
    const minDate = Math.min(...timestamps);
    const maxDate = Math.max(...timestamps);
    const dateRange = maxDate - minDate || 1;

    // Collect all values to build a shared Y scale
    const allValues = [
      ...sorted.map(m => m.weightKg).filter((v): v is number => v != null),
      ...sorted.map(m => m.fatPercentage).filter((v): v is number => v != null),
      ...sorted.map(m => m.muscleMassKg).filter((v): v is number => v != null),
    ];

    if (allValues.length === 0) {
      this.hasChartData = false;
      return;
    }

    const globalMin = Math.min(...allValues);
    const globalMax = Math.max(...allValues);
    const pad = (globalMax - globalMin) * 0.15 || 2;
    const yLow = globalMin - pad;
    const yHigh = globalMax + pad;

    const toX = (ts: number) => (ts - minDate) / dateRange * 94 + 3;
    const toY = (v: number) => 35 - ((v - yLow) / (yHigh - yLow)) * 28;

    this.chartWeightPoints = sorted
      .filter(m => m.weightKg != null)
      .map(m => { const ts = new Date(m.measuredAt).getTime(); return { x: toX(ts), y: toY(m.weightKg!), rawValue: m.weightKg!, timestamp: ts }; });

    this.chartFatPoints = sorted
      .filter(m => m.fatPercentage != null)
      .map(m => { const ts = new Date(m.measuredAt).getTime(); return { x: toX(ts), y: toY(m.fatPercentage!), rawValue: m.fatPercentage!, timestamp: ts }; });

    this.chartMusclePoints = sorted
      .filter(m => m.muscleMassKg != null)
      .map(m => { const ts = new Date(m.measuredAt).getTime(); return { x: toX(ts), y: toY(m.muscleMassKg!), rawValue: m.muscleMassKg!, timestamp: ts }; });

    if (this.chartWeightPoints.length > 0) {
      this.weightPath = this.buildSvgPath(this.chartWeightPoints);
      const first = this.chartWeightPoints[0];
      const last = this.chartWeightPoints[this.chartWeightPoints.length - 1];
      this.weightFillPath = `${this.weightPath} L${last.x.toFixed(2)},38 L${first.x.toFixed(2)},38 Z`;
    } else {
      this.weightPath = '';
      this.weightFillPath = '';
    }

    this.fatPath = this.chartFatPoints.length > 0 ? this.buildSvgPath(this.chartFatPoints) : '';
    this.musclePath = this.chartMusclePoints.length > 0 ? this.buildSvgPath(this.chartMusclePoints) : '';

    this.axisLabels = this.buildAxisLabels(minDate, maxDate);
  }

  private buildSvgPath(points: ChartPoint[]): string {
    if (points.length === 0) return '';
    if (points.length === 1) {
      const p = points[0];
      return `M${p.x.toFixed(2)},${p.y.toFixed(2)} L${(p.x + 0.01).toFixed(2)},${p.y.toFixed(2)}`;
    }

    let d = `M${points[0].x.toFixed(2)},${points[0].y.toFixed(2)}`;
    for (let i = 1; i < points.length; i++) {
      const p0 = points[i - 1];
      const p1 = points[i];
      const cpX = ((p0.x + p1.x) / 2).toFixed(2);
      d += ` C${cpX},${p0.y.toFixed(2)} ${cpX},${p1.y.toFixed(2)} ${p1.x.toFixed(2)},${p1.y.toFixed(2)}`;
    }
    return d;
  }

  private buildAxisLabels(minDate: number, maxDate: number): string[] {
    const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    const start = new Date(minDate);
    const end = new Date(maxDate);

    const labels: string[] = [];
    const current = new Date(start.getFullYear(), start.getMonth(), 1);
    const endMonth = new Date(end.getFullYear(), end.getMonth(), 1);

    while (current <= endMonth) {
      labels.push(months[current.getMonth()]);
      current.setMonth(current.getMonth() + 1);
    }

    if (labels.length === 0) return ['', '', '', 'TODAY'];
    labels[labels.length - 1] = 'TODAY';

    if (labels.length >= 4) {
      const len = labels.length;
      return [labels[0], labels[Math.round(len / 3)], labels[Math.round((len * 2) / 3)], 'TODAY'];
    }

    while (labels.length < 4) labels.unshift('');
    return labels;
  }

  private interpolateY(points: ChartPoint[], svgX: number): number | null {
    if (points.length === 0) return null;
    if (points.length === 1) return points[0].y;
    if (svgX <= points[0].x) return points[0].y;
    if (svgX >= points[points.length - 1].x) return points[points.length - 1].y;

    for (let i = 1; i < points.length; i++) {
      if (svgX <= points[i].x) {
        const p0 = points[i - 1];
        const p1 = points[i];
        const t = (svgX - p0.x) / (p1.x - p0.x);
        return p0.y + (p1.y - p0.y) * t;
      }
    }
    return points[points.length - 1].y;
  }

  private interpolateRawValue(points: ChartPoint[], svgX: number): number | null {
    if (points.length === 0) return null;
    if (points.length === 1) return points[0].rawValue;
    if (svgX <= points[0].x) return points[0].rawValue;
    if (svgX >= points[points.length - 1].x) return points[points.length - 1].rawValue;

    for (let i = 1; i < points.length; i++) {
      if (svgX <= points[i].x) {
        const p0 = points[i - 1];
        const p1 = points[i];
        const t = (svgX - p0.x) / (p1.x - p0.x);
        return p0.rawValue + (p1.rawValue - p0.rawValue) * t;
      }
    }
    return points[points.length - 1].rawValue;
  }

  private interpolateTimestamp(points: ChartPoint[], svgX: number): number | null {
    if (points.length === 0) return null;
    if (points.length === 1) return points[0].timestamp;
    if (svgX <= points[0].x) return points[0].timestamp;
    if (svgX >= points[points.length - 1].x) return points[points.length - 1].timestamp;

    for (let i = 1; i < points.length; i++) {
      if (svgX <= points[i].x) {
        const p0 = points[i - 1];
        const p1 = points[i];
        const t = (svgX - p0.x) / (p1.x - p0.x);
        return p0.timestamp + (p1.timestamp - p0.timestamp) * t;
      }
    }
    return points[points.length - 1].timestamp;
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

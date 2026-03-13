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
  date: string;
  time: string;
  weight: string;
  bodyFat: string;
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

  ngOnInit(): void {
    this.loadData();
  }

  goToLogNewData(): void {
    this.router.navigate(['/body-data/log-body-metrics']);
  }

  private loadData(): void {
    forkJoin({
      latestMeasurement: this.bodyMeasurementService.getLatest().pipe(catchError(() => of(null))),
      measurements: this.bodyMeasurementService.getAll().pipe(catchError(() => of([]))),
      latestBloodPressure: this.bloodPressureService.getLatest().pipe(catchError(() => of(null)))
    }).subscribe(({ latestMeasurement, measurements, latestBloodPressure }) => {
      const effectiveLatestMeasurement = latestMeasurement ?? measurements[0] ?? null;

      this.summaryCards = this.buildSummaryCards(effectiveLatestMeasurement);
      this.detailMetrics = this.buildDetailMetrics(effectiveLatestMeasurement, latestBloodPressure);
      this.historyItems = this.buildHistoryItems(measurements);
      this.lastUpdatedText = this.resolveLastUpdated(effectiveLatestMeasurement, latestBloodPressure);
      this.cdr.detectChanges();
    });
  }

  private buildSummaryCards(latestMeasurement: BodyMeasurementEntry | null): SummaryCard[] {
    return [
      this.createSummaryCard('Weight', latestMeasurement?.weightKg, 'kg'),
      this.createSummaryCard('Body Fat', latestMeasurement?.fatPercentage, '%'),
      this.createSummaryCard('Muscle Mass', latestMeasurement?.muscleMassKg, 'kg'),
      this.createSummaryCard('BMI', latestMeasurement?.bmi)
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
        date: this.formatLongDate(item.measuredAt),
        time: '08:00 AM',
        weight: item.weightKg != null ? `${this.formatNumber(item.weightKg)} kg` : 'No data',
        bodyFat: item.fatPercentage != null ? `${this.formatNumber(item.fatPercentage)}%` : 'No data'
      }));
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

  private createSummaryCard(label: string, value: number | undefined, unit = ''): SummaryCard {
    return {
      label,
      value: value != null ? this.formatNumber(value) : '-',
      unit,
      change: 'Stable',
      trend: 'flat'
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

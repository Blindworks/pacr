import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BloodPressureEntry, BloodPressureService } from '../../../services/blood-pressure.service';
import { ProOverlay } from '../../shared/pro-overlay/pro-overlay';
import { BodyMeasurementEntry, BodyMeasurementService } from '../../../services/body-measurement.service';
import { SleepDataEntry, SleepDataService } from '../../../services/sleep-data.service';

interface LatestEntryMetric {
  label: string;
  value: string;
  trend: 'up' | 'down';
}

@Component({
  selector: 'app-log-body-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule, ProOverlay, TranslateModule],
  templateUrl: './log-body-metrics.html',
  styleUrl: './log-body-metrics.scss'
})
export class LogBodyMetrics {
  @ViewChild('dateDialog') dateDialog?: ElementRef<HTMLDialogElement>;
  @ViewChild('savedDialog') savedDialog?: ElementRef<HTMLDialogElement>;
  @ViewChild('entryTimeInput') entryTimeInput?: ElementRef<HTMLInputElement>;

  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly bodyMeasurementService = inject(BodyMeasurementService);
  private readonly bloodPressureService = inject(BloodPressureService);
  private readonly sleepDataService = inject(SleepDataService);
  private readonly now = new Date();

  entryDate = new Intl.DateTimeFormat('sv-SE').format(this.now);
  entryTime = `${this.now.getHours().toString().padStart(2, '0')}:${this.now.getMinutes().toString().padStart(2, '0')}`;
  weight = '';
  bodyFat = '';
  muscleMass = '';
  boneMass = '';
  waterPercentage = '';
  visceralFat = '';
  metabolicAge = '';
  bmi = '';
  restingHeartRate = '';
  bloodPressure = '';
  notes = '';
  saving = false;
  error: string | null = null;

  readonly latestEntryMetrics: LatestEntryMetric[] = [
    { label: 'Weight', value: '75.8 kg', trend: 'up' },
    { label: 'Body Fat', value: '14.5%', trend: 'down' },
    { label: 'Muscle Mass', value: '61.9 kg', trend: 'down' },
    { label: 'Resting HR', value: '61 BPM', trend: 'down' },
  ];

  openDatePicker(): void {
    this.dateDialog?.nativeElement.showModal();
  }

  closeDatePicker(): void {
    this.dateDialog?.nativeElement.close();
  }

  openTimePicker(): void {
    const input = this.entryTimeInput?.nativeElement as HTMLInputElement & { showPicker?: () => void };
    if (!input) {
      return;
    }

    input.focus();
    if (typeof input.showPicker === 'function') {
      input.showPicker();
      return;
    }

    input.click();
  }

  save(): void {
    const payload = this.buildPayload();
    if (!payload) {
      return;
    }

    this.saving = true;
    this.error = null;

    const bodyMeasurementRequest = payload.bodyMeasurement
      ? this.bodyMeasurementService.create(payload.bodyMeasurement)
      : of(null);
    const bloodPressureRequest = payload.bloodPressure
      ? this.bloodPressureService.create(payload.bloodPressure)
      : of(null);
    const sleepDataRequest = payload.sleepData
      ? this.sleepDataService.create(payload.sleepData)
      : of(null);

    forkJoin([bodyMeasurementRequest, bloodPressureRequest, sleepDataRequest]).subscribe({
      next: () => {
        this.saving = false;
        this.savedDialog?.nativeElement.showModal();
      },
      error: () => {
        this.saving = false;
        this.error = this.translate.instant('BODY_DATA.SAVE_ERROR');
      }
    });
  }

  discard(): void {
    this.router.navigate(['/body-data/body-metrics']);
  }

  onDialogBackdropClick(event: MouseEvent): void {
    const dialog = this.dateDialog?.nativeElement;
    if (!dialog) {
      return;
    }

    const rect = dialog.getBoundingClientRect();
    const isOutside = event.clientX < rect.left || event.clientX > rect.right
      || event.clientY < rect.top || event.clientY > rect.bottom;

    if (isOutside) {
      dialog.close();
    }
  }

  closeSavedDialog(): void {
    this.savedDialog?.nativeElement.close();
    this.router.navigate(['/body-data/body-metrics']);
  }

  onSavedDialogBackdropClick(event: MouseEvent): void {
    const dialog = this.savedDialog?.nativeElement;
    if (!dialog) {
      return;
    }

    const rect = dialog.getBoundingClientRect();
    const isOutside = event.clientX < rect.left || event.clientX > rect.right
      || event.clientY < rect.top || event.clientY > rect.bottom;

    if (isOutside) {
      this.closeSavedDialog();
    }
  }

  private buildPayload(): { bodyMeasurement?: BodyMeasurementEntry; bloodPressure?: BloodPressureEntry; sleepData?: SleepDataEntry } | null {
    const bloodPressure = this.parseBloodPressure(this.bloodPressure);
    if (this.bloodPressure.trim() && !bloodPressure) {
      this.error = this.translate.instant('BODY_DATA.BP_FORMAT_ERROR');
      return null;
    }

    const bodyMeasurement: BodyMeasurementEntry = {
      measuredAt: this.entryDate,
      weightKg: this.parseNumber(this.weight),
      fatPercentage: this.parseNumber(this.bodyFat),
      muscleMassKg: this.parseNumber(this.muscleMass),
      boneMassKg: this.parseNumber(this.boneMass),
      waterPercentage: this.parseNumber(this.waterPercentage),
      visceralFatLevel: this.parseInteger(this.visceralFat),
      metabolicAge: this.parseInteger(this.metabolicAge),
      bmi: this.parseNumber(this.bmi),
      notes: this.notes.trim() || undefined
    };

    const hasBodyMeasurementValue = Object.entries(bodyMeasurement)
      .some(([key, value]) => key !== 'measuredAt' && value !== undefined);

    const restingHeartRate = this.parseInteger(this.restingHeartRate);
    const sleepData = restingHeartRate !== undefined ? {
      recordedAt: this.entryDate,
      restingHeartRate
    } : undefined;
    const bloodPressureEntry = bloodPressure ? {
      measuredAt: this.entryDate,
      systolicPressure: bloodPressure.systolic,
      diastolicPressure: bloodPressure.diastolic,
      pulseAtMeasurement: restingHeartRate,
      notes: this.notes.trim() || undefined
    } : undefined;

    if (!hasBodyMeasurementValue && !bloodPressureEntry && !sleepData) {
      this.error = this.translate.instant('BODY_DATA.MIN_ONE_METRIC');
      return null;
    }

    return {
      bodyMeasurement: hasBodyMeasurementValue ? bodyMeasurement : undefined,
      bloodPressure: bloodPressureEntry,
      sleepData
    };
  }

  private parseNumber(value: string): number | undefined {
    const normalized = value.trim().replace(',', '.');
    if (!normalized) {
      return undefined;
    }

    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private parseInteger(value: string): number | undefined {
    const normalized = value.trim();
    if (!normalized) {
      return undefined;
    }

    const parsed = Number(normalized);
    return Number.isInteger(parsed) ? parsed : undefined;
  }

  private parseBloodPressure(value: string): { systolic: number; diastolic: number } | null {
    const normalized = value.trim();
    if (!normalized) {
      return null;
    }

    const match = normalized.match(/^(\d{2,3})\s*\/\s*(\d{2,3})$/);
    if (!match) {
      return null;
    }

    return {
      systolic: Number(match[1]),
      diastolic: Number(match[2])
    };
  }
}

import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface LatestEntryMetric {
  label: string;
  value: string;
  trend: 'up' | 'down';
}

@Component({
  selector: 'app-log-body-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './log-body-metrics.html',
  styleUrl: './log-body-metrics.scss'
})
export class LogBodyMetrics {
  @ViewChild('dateDialog') dateDialog?: ElementRef<HTMLDialogElement>;
  @ViewChild('entryTimeInput') entryTimeInput?: ElementRef<HTMLInputElement>;

  entryDate = '2023-10-24';
  entryTime = '08:30';
  weight = '';
  bodyFat = '';
  muscleMass = '';
  boneMass = '';
  waterPercentage = '';
  visceralFat = '';
  metabolicAge = '';
  waist = '';
  restingHeartRate = '';
  bloodPressure = '';
  notes = '';

  readonly latestEntryMetrics: LatestEntryMetric[] = [
    { label: 'Weight', value: '75.8 kg', trend: 'up' },
    { label: 'Body Fat', value: '14.5%', trend: 'down' },
    { label: 'Muscle Mass', value: '61.9 kg', trend: 'down' },
    { label: 'Waist', value: '82.5 cm', trend: 'down' },
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
}

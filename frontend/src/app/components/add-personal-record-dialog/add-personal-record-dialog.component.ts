import { Component, ElementRef, EventEmitter, Output, ViewChild, inject } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CreatePersonalRecordRequest,
  PersonalRecordService,
} from '../../services/personal-record.service';

interface DistanceOption {
  label: string;
  distanceKm: number | null;
  distanceLabel: string;
}

const PRESET_OPTIONS: DistanceOption[] = [
  { label: '5K', distanceKm: 5.0, distanceLabel: '5K' },
  { label: '10K', distanceKm: 10.0, distanceLabel: '10K' },
  { label: 'Halbmarathon', distanceKm: 21.0975, distanceLabel: 'Halbmarathon' },
  { label: 'Marathon', distanceKm: 42.195, distanceLabel: 'Marathon' },
  { label: 'Eigene Distanz', distanceKm: null, distanceLabel: '' },
];

@Component({
  selector: 'app-add-personal-record-dialog',
  standalone: true,
  imports: [NgIf, NgFor, FormsModule],
  templateUrl: './add-personal-record-dialog.component.html',
  styleUrl: './add-personal-record-dialog.component.scss',
})
export class AddPersonalRecordDialogComponent {
  @ViewChild('dialog') private dialogEl!: ElementRef<HTMLDialogElement>;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly service = inject(PersonalRecordService);

  readonly distanceOptions = PRESET_OPTIONS;

  selectedOption: DistanceOption | null = null;
  customDistanceKm = '';
  customDistanceLabel = '';
  goalTimeInput = '';
  saving = false;
  error: string | null = null;

  get isCustomDistance(): boolean {
    return this.selectedOption?.distanceKm === null;
  }

  get canSave(): boolean {
    if (!this.selectedOption) return false;
    if (this.isCustomDistance) {
      const km = parseFloat(this.customDistanceKm);
      if (!km || km <= 0) return false;
      if (!this.customDistanceLabel.trim()) return false;
    }
    return true;
  }

  open(): void {
    this.reset();
    this.dialogEl.nativeElement.showModal();
  }

  close(emitCancel = true): void {
    this.dialogEl.nativeElement.close();
    if (emitCancel) this.cancelled.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogEl.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom;
    if (outside) this.close(true);
  }

  onCancel(): void {
    this.close(true);
  }

  onSave(): void {
    if (!this.canSave) return;

    const distanceKm = this.isCustomDistance
      ? parseFloat(this.customDistanceKm)
      : this.selectedOption!.distanceKm!;

    const distanceLabel = this.isCustomDistance
      ? this.customDistanceLabel.trim()
      : this.selectedOption!.distanceLabel;

    const goalTimeSeconds = this.parseTime(this.goalTimeInput);

    const request: CreatePersonalRecordRequest = {
      distanceKm,
      distanceLabel,
      ...(goalTimeSeconds !== null ? { goalTimeSeconds } : {}),
    };

    this.saving = true;
    this.error = null;

    this.service.createPersonalRecord(request).subscribe({
      next: () => {
        this.saving = false;
        this.dialogEl.nativeElement.close();
        this.saved.emit();
      },
      error: (err: { status?: number }) => {
        this.saving = false;
        this.error = err.status === 409
          ? 'Diese Distanz existiert bereits.'
          : 'Speichern fehlgeschlagen. Bitte versuche es erneut.';
      },
    });
  }

  compareOptions(a: DistanceOption | null, b: DistanceOption | null): boolean {
    if (a === null || b === null) return a === b;
    return a.label === b.label;
  }

  parseTime(input: string): number | null {
    const trimmed = input.trim();
    if (!trimmed) return null;
    const parts = trimmed.split(':').map(Number);
    if (parts.some(isNaN)) return null;
    if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
    if (parts.length === 2) return parts[0] * 60 + parts[1];
    return null;
  }

  private reset(): void {
    this.selectedOption = null;
    this.customDistanceKm = '';
    this.customDistanceLabel = '';
    this.goalTimeInput = '';
    this.saving = false;
    this.error = null;
  }
}

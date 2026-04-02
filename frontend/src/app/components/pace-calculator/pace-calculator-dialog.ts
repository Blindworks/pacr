import { Component, ElementRef, ViewChild, effect, inject, OnDestroy } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaceCalculatorService } from '../../services/pace-calculator.service';
import { TranslateModule } from '@ngx-translate/core';

type Field = 'distance' | 'time' | 'pace' | 'speed';

interface Split {
  label: string;
  distanceKm: number;
  timeFormatted: string;
}

@Component({
  selector: 'app-pace-calculator-dialog',
  standalone: true,
  imports: [NgFor, NgIf, FormsModule, TranslateModule],
  templateUrl: './pace-calculator-dialog.html',
  styleUrl: './pace-calculator-dialog.scss'
})
export class PaceCalculatorDialog implements OnDestroy {
  @ViewChild('dialog') private dialogRef!: ElementRef<HTMLDialogElement>;

  private readonly service = inject(PaceCalculatorService);

  // Display strings bound to inputs
  distanceDisplay = '';
  timeDisplay = '';
  paceDisplay = '';
  speedDisplay = '';

  // Internal numeric values (null = not set by user)
  private distanceKm: number | null = null;
  private timeSec: number | null = null;
  private paceSec: number | null = null;
  private speedKmh: number | null = null;

  // Track last 2 fields explicitly edited by the user
  private lastEdited: Field[] = [];
  private focusedField: Field | null = null;

  splits: Split[] = [];

  private readonly openEffect = effect(() => {
    const open = this.service.isOpen();
    if (!this.dialogRef) return;
    const dialog = this.dialogRef.nativeElement;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  });

  close(): void {
    this.service.close();
  }

  onBackdropClick(event: MouseEvent): void {
    const rect = this.dialogRef.nativeElement.getBoundingClientRect();
    const outside =
      event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom;
    if (outside) this.close();
  }

  setPreset(distanceKm: number): void {
    this.distanceKm = distanceKm;
    this.distanceDisplay = distanceKm % 1 === 0 ? distanceKm.toString() : distanceKm.toFixed(4).replace(/\.?0+$/, '');
    this.markEdited('distance');
    this.calculate();
  }

  onFocus(field: Field): void {
    this.focusedField = field;
  }

  onBlur(field: Field): void {
    this.focusedField = null;
  }

  onDistanceInput(): void {
    this.distanceKm = parseFloat(this.distanceDisplay) || null;
    if (this.distanceDisplay.trim() === '') this.distanceKm = null;
    this.markEdited('distance');
    this.calculate();
  }

  onTimeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const masked = this.maskTime(input.value);
    input.value = masked;
    this.timeDisplay = masked;
    this.timeSec = this.parseTime(masked);
    this.markEdited('time');
    this.calculate();
  }

  onPaceInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const masked = this.maskPace(input.value);
    input.value = masked;
    this.paceDisplay = masked;
    this.paceSec = this.parsePace(masked);
    this.markEdited('pace');
    this.calculate();
  }

  onSpeedInput(): void {
    this.speedKmh = parseFloat(this.speedDisplay) || null;
    if (this.speedDisplay.trim() === '') this.speedKmh = null;
    this.markEdited('speed');
    this.calculate();
  }

  reset(): void {
    this.distanceKm = null;
    this.timeSec = null;
    this.paceSec = null;
    this.speedKmh = null;
    this.distanceDisplay = '';
    this.timeDisplay = '';
    this.paceDisplay = '';
    this.speedDisplay = '';
    this.lastEdited = [];
    this.splits = [];
  }

  private markEdited(field: Field): void {
    this.lastEdited = [field, ...this.lastEdited.filter(f => f !== field)].slice(0, 2);
  }

  private calculate(): void {
    const recent = this.lastEdited[0];
    if (!recent) { this.updateSplits(); return; }

    const isPaceOrSpeed = recent === 'pace' || recent === 'speed';

    // Pace/speed changes → always update time, never distance.
    // Determine the two source fields for calculation.
    let source1: Field = recent;
    let source2: Field | null = null;

    if (isPaceOrSpeed) {
      // prefer distance as partner; fall back to time
      if (this.distanceKm !== null) {
        source2 = 'distance';
      } else if (this.timeSec !== null) {
        source2 = 'time';
      } else {
        // only pace/speed known — still link pace ↔ speed
        const val = this.getNumeric(recent);
        if (val !== null) {
          if (recent === 'pace') { this.speedKmh = 3600 / val; this.setDisplay('speed', this.speedKmh); }
          else                  { this.paceSec  = 3600 / val; this.setDisplay('pace',  this.paceSec);  }
        }
        this.updateSplits();
        return;
      }
    } else {
      source2 = this.lastEdited[1] ?? null;
    }

    if (!source2) { this.updateSplits(); return; }

    const val1 = this.getNumeric(source1);
    const val2 = this.getNumeric(source2);
    if (val1 === null || val2 === null) { this.updateSplits(); return; }

    const result = this.computeAll({ [source1]: val1, [source2]: val2 });

    const derived = (['distance', 'time', 'pace', 'speed'] as Field[])
      .filter(f => f !== source1 && f !== source2);
    for (const field of derived) {
      if (field === this.focusedField) continue;
      const val = result[field] ?? null;
      this.setNumeric(field, val);
      this.setDisplay(field, val);
    }

    this.updateSplits();
  }

  private computeAll(known: Partial<Record<Field, number>>): Partial<Record<Field, number>> {
    let d = known['distance'] ?? null;
    let t = known['time'] ?? null;
    let p = known['pace'] ?? null;
    let s = known['speed'] ?? null;

    // pace and speed are linked: speed = 3600 / pace
    if (p !== null && s === null) s = 3600 / p;
    if (s !== null && p === null) p = 3600 / s;

    // derive third variable from any two of {distance, time, pace}
    if (d !== null && t !== null && p === null && t > 0) { p = t / d; s = 3600 / p; }
    if (d !== null && p !== null && t === null && d > 0) { t = d * p; }
    if (t !== null && p !== null && d === null && p > 0) { d = t / p; }

    // ensure consistency
    if (p !== null && s === null) s = 3600 / p;
    if (s !== null && p === null) p = 3600 / s;

    return {
      ...(d !== null ? { distance: d } : {}),
      ...(t !== null ? { time: t } : {}),
      ...(p !== null ? { pace: p } : {}),
      ...(s !== null ? { speed: s } : {}),
    };
  }

  private updateSplits(): void {
    const pace = this.paceSec ?? this.getNumeric('pace');
    if (!pace || pace <= 0) {
      this.splits = [];
      return;
    }
    const splitDistances = [
      { label: '1 KM', distanceKm: 1 },
      { label: '5 KM', distanceKm: 5 },
      { label: '10 KM', distanceKm: 10 },
      { label: '21.1 KM', distanceKm: 21.0975 },
      { label: '42.2 KM', distanceKm: 42.195 },
    ];
    this.splits = splitDistances.map(s => ({
      label: s.label,
      distanceKm: s.distanceKm,
      timeFormatted: this.formatTime(s.distanceKm * pace),
    }));
  }

  private getNumeric(field: Field): number | null {
    switch (field) {
      case 'distance': return this.distanceKm;
      case 'time': return this.timeSec;
      case 'pace': return this.paceSec;
      case 'speed': return this.speedKmh;
    }
  }

  private setNumeric(field: Field, val: number | null): void {
    switch (field) {
      case 'distance': this.distanceKm = val; break;
      case 'time': this.timeSec = val; break;
      case 'pace': this.paceSec = val; break;
      case 'speed': this.speedKmh = val; break;
    }
  }

  private setDisplay(field: Field, val: number | null): void {
    switch (field) {
      case 'distance':
        this.distanceDisplay = val !== null ? parseFloat(val.toFixed(3)).toString() : '';
        break;
      case 'time':
        this.timeDisplay = val !== null ? this.formatTime(val) : '';
        break;
      case 'pace':
        this.paceDisplay = val !== null ? this.formatPace(val) : '';
        break;
      case 'speed':
        this.speedDisplay = val !== null ? parseFloat(val.toFixed(1)).toString() : '';
        break;
    }
  }

  // Mask digits to "HH:MM:SS" as user types
  private maskTime(raw: string): string {
    const digits = raw.replace(/\D/g, '').slice(0, 6);
    if (digits.length <= 2) return digits;
    if (digits.length <= 4) return `${digits.slice(0, 2)}:${digits.slice(2)}`;
    return `${digits.slice(0, 2)}:${digits.slice(2, 4)}:${digits.slice(4)}`;
  }

  // Mask digits to "MM:SS" as user types
  private maskPace(raw: string): string {
    const digits = raw.replace(/\D/g, '').slice(0, 4);
    if (digits.length <= 2) return digits;
    return `${digits.slice(0, 2)}:${digits.slice(2)}`;
  }

  // Parse "HH:MM:SS" or "MM:SS" → total seconds
  private parseTime(value: string): number | null {
    const v = value.trim();
    if (!v) return null;
    const parts = v.split(':').map(Number);
    if (parts.some(isNaN)) return null;
    if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
    if (parts.length === 2) return parts[0] * 60 + parts[1];
    return null;
  }

  // Parse "MM:SS" → total seconds per km
  private parsePace(value: string): number | null {
    const v = value.trim();
    if (!v) return null;
    const parts = v.split(':').map(Number);
    if (parts.some(isNaN)) return null;
    if (parts.length === 2) return parts[0] * 60 + parts[1];
    if (parts.length === 1 && !isNaN(parts[0])) return parts[0] * 60;
    return null;
  }

  // Format seconds → "HH:MM:SS"
  private formatTime(totalSec: number): string {
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = Math.floor(totalSec % 60);
    return `${this.pad(h)}:${this.pad(m)}:${this.pad(s)}`;
  }

  // Format seconds/km → "MM:SS"
  private formatPace(secPerKm: number): string {
    const m = Math.floor(secPerKm / 60);
    const s = Math.floor(secPerKm % 60);
    return `${this.pad(m)}:${this.pad(s)}`;
  }

  private pad(n: number): string {
    return n.toString().padStart(2, '0');
  }

  ngOnDestroy(): void {}
}

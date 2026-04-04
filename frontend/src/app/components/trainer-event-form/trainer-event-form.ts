import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TrainerEventService, CreateGroupEventRequest, UpdateGroupEventRequest } from '../../services/trainer-event.service';
import { GroupEventDto } from '../../services/group-event.service';

@Component({
  selector: 'app-trainer-event-form',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './trainer-event-form.html',
  styleUrl: './trainer-event-form.scss'
})
export class TrainerEventForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trainerService = inject(TrainerEventService);
  readonly translate = inject(TranslateService);

  form!: FormGroup;
  isEditMode = signal(false);
  eventId = signal<number | null>(null);
  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);

  readonly difficulties = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
  readonly currencies = ['EUR', 'USD', 'GBP', 'CHF'];
  readonly frequencies = ['WEEKLY', 'DAILY', 'MONTHLY', 'YEARLY'];
  readonly weekdays = [
    { value: 'MO', labelKey: 'TRAINER_EVENTS.DAY_MO' },
    { value: 'TU', labelKey: 'TRAINER_EVENTS.DAY_TU' },
    { value: 'WE', labelKey: 'TRAINER_EVENTS.DAY_WE' },
    { value: 'TH', labelKey: 'TRAINER_EVENTS.DAY_TH' },
    { value: 'FR', labelKey: 'TRAINER_EVENTS.DAY_FR' },
    { value: 'SA', labelKey: 'TRAINER_EVENTS.DAY_SA' },
    { value: 'SU', labelKey: 'TRAINER_EVENTS.DAY_SU' }
  ];
  readonly monthlyPositions = [1, 2, 3, 4, -1];

  ngOnInit(): void {
    this.initForm();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.eventId.set(Number(id));
      this.loadEvent(Number(id));
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(200)]],
      description: [''],
      eventDate: ['', Validators.required],
      startTime: ['', Validators.required],
      endTime: [''],
      locationName: ['', Validators.required],
      latitude: [null],
      longitude: [null],
      distanceKm: [null],
      paceMinSecondsPerKm: [null],
      paceMaxSecondsPerKm: [null],
      maxParticipants: [null],
      costCents: [null],
      costCurrency: ['EUR'],
      difficulty: [null],
      // Recurrence fields
      recurrenceEnabled: [false],
      recurrenceFrequency: ['WEEKLY'],
      recurrenceInterval: [1],
      recurrenceByDay: [[]],
      recurrenceMonthlyPosition: [1],
      recurrenceMonthlyDay: ['MO'],
      recurrenceEndDate: [null]
    });
  }

  private loadEvent(id: number): void {
    this.loading.set(true);
    this.trainerService.getTrainerEvents().subscribe({
      next: events => {
        const event = events.find(e => e.id === id);
        if (event) {
          this.patchForm(event);
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('TRAINER_EVENTS.LOAD_ERROR'));
        this.loading.set(false);
      }
    });
  }

  private patchForm(event: GroupEventDto): void {
    this.form.patchValue({
      title: event.title,
      description: event.description,
      eventDate: event.eventDate,
      startTime: event.startTime,
      endTime: event.endTime,
      locationName: event.locationName,
      latitude: event.latitude,
      longitude: event.longitude,
      distanceKm: event.distanceKm,
      paceMinSecondsPerKm: event.paceMinSecondsPerKm,
      paceMaxSecondsPerKm: event.paceMaxSecondsPerKm,
      maxParticipants: event.maxParticipants,
      costCents: event.costCents,
      costCurrency: event.costCurrency || 'EUR',
      difficulty: event.difficulty
    });

    if (event.rrule) {
      const parsed = this.parseRrule(event.rrule);
      this.form.patchValue({
        recurrenceEnabled: true,
        recurrenceFrequency: parsed.freq,
        recurrenceInterval: parsed.interval,
        recurrenceByDay: parsed.byDay,
        recurrenceMonthlyPosition: parsed.monthlyPos,
        recurrenceMonthlyDay: parsed.monthlyDay,
        recurrenceEndDate: event.recurrenceEndDate
      });
    }
  }

  saveAsDraft(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.save(false);
  }

  saveAndPublish(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.save(true);
  }

  private save(publish: boolean): void {
    this.saving.set(true);
    this.error.set(null);

    const formValue = this.form.value;
    const request: CreateGroupEventRequest = {
      title: formValue.title,
      description: formValue.description || undefined,
      eventDate: formValue.eventDate,
      startTime: formValue.startTime,
      endTime: formValue.endTime || undefined,
      locationName: formValue.locationName,
      latitude: formValue.latitude || undefined,
      longitude: formValue.longitude || undefined,
      distanceKm: formValue.distanceKm || undefined,
      paceMinSecondsPerKm: formValue.paceMinSecondsPerKm || undefined,
      paceMaxSecondsPerKm: formValue.paceMaxSecondsPerKm || undefined,
      maxParticipants: formValue.maxParticipants || undefined,
      costCents: formValue.costCents || undefined,
      costCurrency: formValue.costCurrency || undefined,
      difficulty: formValue.difficulty || undefined,
      rrule: formValue.recurrenceEnabled ? this.buildRrule() : undefined,
      recurrenceEndDate: formValue.recurrenceEnabled && formValue.recurrenceEndDate ? formValue.recurrenceEndDate : undefined
    };

    if (this.isEditMode() && this.eventId()) {
      this.trainerService.updateEvent(this.eventId()!, request as UpdateGroupEventRequest).subscribe({
        next: updated => {
          if (publish && updated.status === 'DRAFT') {
            this.trainerService.publishEvent(updated.id).subscribe({
              next: () => { this.saving.set(false); this.router.navigate(['/trainer/events']); },
              error: () => { this.saving.set(false); this.router.navigate(['/trainer/events']); }
            });
          } else {
            this.saving.set(false);
            this.router.navigate(['/trainer/events']);
          }
        },
        error: () => {
          this.error.set(this.translate.instant('TRAINER_EVENTS.SAVE_ERROR'));
          this.saving.set(false);
        }
      });
    } else {
      this.trainerService.createEvent(request).subscribe({
        next: created => {
          if (publish) {
            this.trainerService.publishEvent(created.id).subscribe({
              next: () => { this.saving.set(false); this.router.navigate(['/trainer/events']); },
              error: () => { this.saving.set(false); this.router.navigate(['/trainer/events']); }
            });
          } else {
            this.saving.set(false);
            this.router.navigate(['/trainer/events']);
          }
        },
        error: () => {
          this.error.set(this.translate.instant('TRAINER_EVENTS.SAVE_ERROR'));
          this.saving.set(false);
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/trainer/events']);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!field && field.invalid && field.touched;
  }

  formatPaceDisplay(seconds: number | null): string {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  parsePaceInput(value: string): number | null {
    const match = value.match(/^(\d{1,2}):(\d{2})$/);
    if (!match) return null;
    return parseInt(match[1]) * 60 + parseInt(match[2]);
  }

  onPaceInput(field: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const seconds = this.parsePaceInput(input.value);
    this.form.get(field)?.setValue(seconds);
  }

  toggleWeekday(day: string): void {
    const current: string[] = this.form.get('recurrenceByDay')?.value || [];
    const idx = current.indexOf(day);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(day);
    }
    this.form.get('recurrenceByDay')?.setValue([...current]);
  }

  isDaySelected(day: string): boolean {
    const current: string[] = this.form.get('recurrenceByDay')?.value || [];
    return current.includes(day);
  }

  private buildRrule(): string {
    const freq = this.form.get('recurrenceFrequency')?.value || 'WEEKLY';
    const interval = this.form.get('recurrenceInterval')?.value || 1;
    let rrule = `FREQ=${freq}`;

    if (interval > 1) {
      rrule += `;INTERVAL=${interval}`;
    }

    if (freq === 'WEEKLY') {
      const days: string[] = this.form.get('recurrenceByDay')?.value || [];
      if (days.length > 0) {
        rrule += `;BYDAY=${days.join(',')}`;
      }
    } else if (freq === 'MONTHLY') {
      const pos = this.form.get('recurrenceMonthlyPosition')?.value || 1;
      const day = this.form.get('recurrenceMonthlyDay')?.value || 'MO';
      rrule += `;BYDAY=${pos}${day}`;
    }

    return rrule;
  }

  private parseRrule(rrule: string): { freq: string; interval: number; byDay: string[]; monthlyPos: number; monthlyDay: string } {
    const parts: Record<string, string> = {};
    for (const part of rrule.split(';')) {
      const [key, value] = part.split('=', 2);
      if (key && value) parts[key] = value;
    }

    const freq = parts['FREQ'] || 'WEEKLY';
    const interval = parts['INTERVAL'] ? parseInt(parts['INTERVAL'], 10) : 1;
    let byDay: string[] = [];
    let monthlyPos = 1;
    let monthlyDay = 'MO';

    if (parts['BYDAY']) {
      if (freq === 'MONTHLY') {
        // Parse positional like "2TU" or "-1FR"
        const match = parts['BYDAY'].match(/^(-?\d+)([A-Z]{2})$/);
        if (match) {
          monthlyPos = parseInt(match[1], 10);
          monthlyDay = match[2];
        }
      } else {
        byDay = parts['BYDAY'].split(',');
      }
    }

    return { freq, interval, byDay, monthlyPos, monthlyDay };
  }

  getPositionLabel(pos: number): string {
    if (pos === -1) return this.translate.instant('TRAINER_EVENTS.POS_LAST');
    const keys = ['', 'TRAINER_EVENTS.POS_FIRST', 'TRAINER_EVENTS.POS_SECOND', 'TRAINER_EVENTS.POS_THIRD', 'TRAINER_EVENTS.POS_FOURTH'];
    return this.translate.instant(keys[pos] || '');
  }
}

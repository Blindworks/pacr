import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TrainerEventService, CreateGroupEventRequest, UpdateGroupEventRequest } from '../../services/trainer-event.service';
import { GroupEventDto, GroupEventService } from '../../services/group-event.service';
import { GeocodingService } from '../../services/geocoding.service';
import { LocationPickerDialogComponent } from '../location-picker-dialog/location-picker-dialog';

@Component({
  selector: 'app-trainer-event-form',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, TranslateModule, LocationPickerDialogComponent],
  templateUrl: './trainer-event-form.html',
  styleUrl: './trainer-event-form.scss'
})
export class TrainerEventForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trainerService = inject(TrainerEventService);
  private readonly groupEventService = inject(GroupEventService);
  private readonly geocodingService = inject(GeocodingService);
  readonly translate = inject(TranslateService);

  @ViewChild('locationPicker') locationPicker!: LocationPickerDialogComponent;

  form!: FormGroup;
  isEditMode = signal(false);
  eventId = signal<number | null>(null);
  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  geocoding = signal(false);
  geocodeError = signal<string | null>(null);

  selectedImageFile = signal<File | null>(null);
  imagePreviewUrl = signal<string | null>(null);
  existingImageFilename = signal<string | null>(null);
  imageMarkedForDeletion = signal(false);
  imageError = signal<string | null>(null);
  private imageObjectUrl: string | null = null;
  private readonly allowedImageTypes = ['image/png', 'image/jpeg', 'image/webp'];
  private readonly maxImageSizeBytes = 5 * 1024 * 1024;

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

    if (event.eventImageFilename) {
      this.existingImageFilename.set(event.eventImageFilename);
      this.groupEventService.getEventImage(event.id).subscribe({
        next: blob => {
          if (!blob) return;
          this.revokeImageObjectUrl();
          this.imageObjectUrl = URL.createObjectURL(blob);
          this.imagePreviewUrl.set(this.imageObjectUrl);
        },
        error: () => {}
      });
    }
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!this.allowedImageTypes.includes(file.type)) {
      this.imageError.set(this.translate.instant('TRAINER_EVENTS.IMAGE_INVALID_TYPE'));
      input.value = '';
      return;
    }
    if (file.size > this.maxImageSizeBytes) {
      this.imageError.set(this.translate.instant('TRAINER_EVENTS.IMAGE_TOO_LARGE'));
      input.value = '';
      return;
    }

    this.imageError.set(null);
    this.imageMarkedForDeletion.set(false);
    this.selectedImageFile.set(file);

    this.revokeImageObjectUrl();
    this.imageObjectUrl = URL.createObjectURL(file);
    this.imagePreviewUrl.set(this.imageObjectUrl);
  }

  removeImage(): void {
    this.selectedImageFile.set(null);
    this.imageError.set(null);
    this.revokeImageObjectUrl();
    this.imagePreviewUrl.set(null);
    if (this.existingImageFilename()) {
      this.imageMarkedForDeletion.set(true);
    }
  }

  hasImage(): boolean {
    return !!this.imagePreviewUrl();
  }

  private revokeImageObjectUrl(): void {
    if (this.imageObjectUrl) {
      URL.revokeObjectURL(this.imageObjectUrl);
      this.imageObjectUrl = null;
    }
  }

  ngOnDestroy(): void {
    this.revokeImageObjectUrl();
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
          this.applyImageChanges(updated.id, () => {
            if (publish && updated.status === 'DRAFT') {
              this.trainerService.publishEvent(updated.id).subscribe({
                next: () => this.finishSave(),
                error: () => this.finishSave()
              });
            } else {
              this.finishSave();
            }
          });
        },
        error: () => {
          this.error.set(this.translate.instant('TRAINER_EVENTS.SAVE_ERROR'));
          this.saving.set(false);
        }
      });
    } else {
      this.trainerService.createEvent(request).subscribe({
        next: created => {
          this.applyImageChanges(created.id, () => {
            if (publish) {
              this.trainerService.publishEvent(created.id).subscribe({
                next: () => this.finishSave(),
                error: () => this.finishSave()
              });
            } else {
              this.finishSave();
            }
          });
        },
        error: () => {
          this.error.set(this.translate.instant('TRAINER_EVENTS.SAVE_ERROR'));
          this.saving.set(false);
        }
      });
    }
  }

  private applyImageChanges(eventId: number, done: () => void): void {
    const file = this.selectedImageFile();
    if (file) {
      this.trainerService.uploadEventImage(eventId, file).subscribe({
        next: () => done(),
        error: () => {
          this.imageError.set(this.translate.instant('TRAINER_EVENTS.IMAGE_UPLOAD_ERROR'));
          done();
        }
      });
      return;
    }
    if (this.imageMarkedForDeletion() && this.existingImageFilename()) {
      this.trainerService.deleteEventImage(eventId).subscribe({
        next: () => done(),
        error: () => done()
      });
      return;
    }
    done();
  }

  private finishSave(): void {
    this.saving.set(false);
    this.router.navigate(['/trainer/events']);
  }

  geocodeLocation(): void {
    const query = this.form.get('locationName')?.value;
    if (!query || query.trim().length < 2) return;

    this.geocoding.set(true);
    this.geocodeError.set(null);

    this.geocodingService.geocode(query).subscribe({
      next: results => {
        this.geocoding.set(false);
        if (results.length > 0) {
          const first = results[0];
          this.form.patchValue({
            latitude: Math.round(parseFloat(first.lat) * 1000000) / 1000000,
            longitude: Math.round(parseFloat(first.lon) * 1000000) / 1000000
          });
          this.geocodeError.set(null);
        } else {
          this.geocodeError.set(this.translate.instant('TRAINER_EVENTS.GEOCODE_NOT_FOUND'));
        }
      },
      error: () => {
        this.geocoding.set(false);
        this.geocodeError.set(this.translate.instant('TRAINER_EVENTS.GEOCODE_NOT_FOUND'));
      }
    });
  }

  openLocationPicker(): void {
    const lat = this.form.get('latitude')?.value;
    const lng = this.form.get('longitude')?.value;
    this.locationPicker.open(lat, lng);
  }

  onLocationPicked(coords: { lat: number; lng: number }): void {
    this.form.patchValue({
      latitude: coords.lat,
      longitude: coords.lng
    });
    this.geocodeError.set(null);
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

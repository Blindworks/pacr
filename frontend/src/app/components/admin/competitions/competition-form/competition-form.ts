import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { CompetitionService, CompetitionFormat } from '../../../../services/competition.service';
import { GeocodingService } from '../../../../services/geocoding.service';
import { LocationPickerDialogComponent } from '../../../location-picker-dialog/location-picker-dialog';
import {
  CompetitionImageCategory,
  COMPETITION_IMAGE_COUNTS,
  categoryForType,
  defaultImageIndex,
  imagePath,
} from '../../../competitions/competition-images';

@Component({
  selector: 'app-competition-form',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, LocationPickerDialogComponent],
  templateUrl: './competition-form.html',
  styleUrl: './competition-form.scss'
})
export class CompetitionForm implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(CompetitionService);
  private geocodingService = inject(GeocodingService);
  private translate = inject(TranslateService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  @ViewChild('locationPicker') locationPicker!: LocationPickerDialogComponent;

  editId = signal<number | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  hasError = signal(false);
  geocoding = signal(false);
  geocodeError = signal<string | null>(null);

  imageIndex = signal<number | null>(null);

  form = this.fb.group({
    name: ['', Validators.required],
    date: ['', Validators.required],
    location: [''],
    latitude: [null as number | null],
    longitude: [null as number | null],
    description: [''],
    organizerUrl: [''],
    formats: this.fb.array([])
  });

  readonly competitionTypes = [
    { value: 'FIVE_K', label: '5K' },
    { value: 'TEN_K', label: '10K' },
    { value: 'TWENTY_K', label: '20K' },
    { value: 'HALF_MARATHON', label: 'Halbmarathon' },
    { value: 'THIRTY_K', label: '30K' },
    { value: 'FORTY_K', label: '40K' },
    { value: 'MARATHON', label: 'Marathon' },
    { value: 'FIFTY_K', label: '50K' },
    { value: 'HUNDRED_K', label: '100K' },
    { value: 'BACKYARD_ULTRA', label: 'Backyard Ultra' },
    { value: 'CATCHER_CAR', label: 'Catcher Car' },
    { value: 'OTHER', label: 'Sonstige' }
  ];

  get isEdit(): boolean { return this.editId() !== null; }
  get formatsArray(): FormArray { return this.form.get('formats') as FormArray; }

  private loadedCompetitionId: number | null = null;
  private previousCategory: CompetitionImageCategory = 'city';

  private firstFormatLabel(): string | undefined {
    const firstType = this.formatsArray.at(0)?.get('type')?.value as string | undefined;
    if (!firstType) return undefined;
    return this.competitionTypes.find(t => t.value === firstType)?.label ?? firstType;
  }

  currentCategory(): CompetitionImageCategory {
    return categoryForType(this.firstFormatLabel());
  }

  displayedImageIndex(): number {
    const category = this.currentCategory();
    const idx = this.imageIndex();
    const count = COMPETITION_IMAGE_COUNTS[category];
    if (idx != null && idx >= 1 && idx <= count) return idx;
    return defaultImageIndex(category, this.loadedCompetitionId ?? 0);
  }

  imagePreviewUrl(): string {
    return imagePath(this.currentCategory(), this.displayedImageIndex());
  }

  imageCount(): number {
    return COMPETITION_IMAGE_COUNTS[this.currentCategory()];
  }

  rotateImage(direction: 1 | -1): void {
    const count = this.imageCount();
    const current = this.displayedImageIndex();
    const next = ((current - 1 + direction + count) % count) + 1;
    this.imageIndex.set(next);
  }

  resetImage(): void {
    this.imageIndex.set(null);
  }

  onFormatTypeChanged(): void {
    const newCategory = this.currentCategory();
    if (newCategory !== this.previousCategory) {
      this.imageIndex.set(null);
      this.previousCategory = newCategory;
    }
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('compId');
    if (idParam) {
      const id = parseInt(idParam, 10);
      this.editId.set(id);
      this.loadCompetition(id);
    }
  }

  private loadCompetition(id: number): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.service.getById(id).subscribe({
      next: (c) => {
        if (!c) return;
        this.form.patchValue({
          name: c.name,
          date: c.date ? c.date.substring(0, 10) : '',
          location: c.location ?? '',
          latitude: c.latitude ?? null,
          longitude: c.longitude ?? null,
          description: c.description ?? '',
          organizerUrl: c.organizerUrl ?? ''
        });
        this.imageIndex.set(c.imageIndex ?? null);
        this.loadedCompetitionId = c.id ?? null;
        // Load existing formats into FormArray
        this.formatsArray.clear();
        if (c.formats && c.formats.length > 0) {
          for (const f of c.formats) {
            this.formatsArray.push(this.createFormatGroup(f));
          }
        }
        this.previousCategory = this.currentCategory();
      },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  private createFormatGroup(format?: CompetitionFormat): FormGroup {
    // API returns display name (e.g. "Halbmarathon") via @JsonValue, but select options use enum value (e.g. "HALF_MARATHON")
    const typeValue = format?.type
      ? (this.competitionTypes.find(t => t.value === format.type || t.label === format.type)?.value ?? format.type)
      : '';
    return this.fb.group({
      id: [format?.id ?? null],
      type: [typeValue, Validators.required],
      startTime: [format?.startTime ?? ''],
      startDate: [format?.startDate ? format.startDate.substring(0, 10) : ''],
      description: [format?.description ?? '']
    });
  }

  addFormat(): void {
    this.formatsArray.push(this.createFormatGroup());
  }

  removeFormat(index: number): void {
    this.formatsArray.removeAt(index);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const formats = (v.formats || [])
      .filter((f: any) => f.type)
      .map((f: any) => ({
        id: f.id || undefined,
        type: f.type,
        startTime: f.startTime || undefined,
        startDate: f.startDate || undefined,
        description: f.description || undefined
      }));
    const payload: any = {
      name: v.name ?? undefined,
      date: v.date ?? undefined,
      location: v.location || undefined,
      latitude: v.latitude || undefined,
      longitude: v.longitude || undefined,
      description: v.description || undefined,
      organizerUrl: v.organizerUrl || undefined,
      imageIndex: this.imageIndex(),
      formats: formats.length > 0 ? formats : undefined
    };
    this.isSaving.set(true);
    const op$ = this.isEdit
      ? this.service.update(this.editId()!, payload)
      : this.service.create(payload);

    op$.subscribe({
      next: (result) => { if (result) this.router.navigate(['/admin/competitions']); },
      error: () => { this.hasError.set(true); this.isSaving.set(false); },
      complete: () => this.isSaving.set(false)
    });
  }

  cancel(): void { this.router.navigate(['/admin/competitions']); }

  geocodeLocation(): void {
    const query = this.form.get('location')?.value;
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
}

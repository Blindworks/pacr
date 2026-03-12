import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';

import { CompetitionService } from '../../../../services/competition.service';

@Component({
  selector: 'app-competition-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './competition-form.html',
  styleUrl: './competition-form.scss'
})
export class CompetitionForm implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(CompetitionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  editId = signal<number | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  hasError = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    date: ['', Validators.required],
    type: [''],
    location: [''],
    description: ['']
  });

  readonly competitionTypes = [
    { value: 'FIVE_K', label: '5K' },
    { value: 'TEN_K', label: '10K' },
    { value: 'HALF_MARATHON', label: 'Halbmarathon' },
    { value: 'MARATHON', label: 'Marathon' },
    { value: 'FIFTY_K', label: '50K' },
    { value: 'HUNDRED_K', label: '100K' },
    { value: 'BACKYARD_ULTRA', label: 'Backyard Ultra' },
    { value: 'CATCHER_CAR', label: 'Catcher Car' },
    { value: 'OTHER', label: 'Sonstige' }
  ];

  get isEdit(): boolean { return this.editId() !== null; }

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
        const typeEntry = this.competitionTypes.find(t => t.label === c.type || t.value === c.type);
        this.form.patchValue({
          name: c.name,
          date: c.date ?? '',
          type: typeEntry?.value ?? '',
          location: c.location ?? '',
          description: c.description ?? ''
        });
      },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const payload = {
      name: v.name ?? undefined,
      date: v.date ?? undefined,
      type: v.type || undefined,
      location: v.location || undefined,
      description: v.description || undefined
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
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';

import { TrainingPlanService } from '../../../../services/training-plan.service';

@Component({
  selector: 'app-plan-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './plan-form.html',
  styleUrl: './plan-form.scss'
})
export class PlanForm implements OnInit {
  private fb = inject(FormBuilder);
  private planService = inject(TrainingPlanService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  editId = signal<number | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  hasError = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    competitionType: [''],
    targetTime: [''],
    prerequisites: ['']
  });

  get isEdit(): boolean { return this.editId() !== null; }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('planId');
    if (idParam) {
      const id = parseInt(idParam, 10);
      this.editId.set(id);
      this.loadPlan(id);
    }
  }

  private loadPlan(id: number): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.planService.getById(id).subscribe({
      next: (p) => {
        if (!p) return;
        this.form.patchValue({
          name: p.name,
          description: p.description ?? '',
          competitionType: p.competitionType ?? '',
          targetTime: p.targetTime ?? '',
          prerequisites: p.prerequisites ?? ''
        });
      },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const payload: Partial<import('../../../../services/training-plan.service').TrainingPlan> = {
      name: v.name ?? undefined,
      description: v.description ?? undefined,
      competitionType: v.competitionType ?? undefined,
      targetTime: v.targetTime ?? undefined,
      prerequisites: v.prerequisites ?? undefined
    };
    this.isSaving.set(true);
    const op$ = this.isEdit
      ? this.planService.update(this.editId()!, payload)
      : this.planService.create(payload);

    op$.subscribe({
      next: (result) => { if (result) this.router.navigate(['/admin/plans']); },
      error: () => { this.hasError.set(true); this.isSaving.set(false); },
      complete: () => this.isSaving.set(false)
    });
  }

  cancel(): void { this.router.navigate(['/admin/plans']); }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { TrainingService } from '../../../../services/training.service';
import { TrainingPlanService, TrainingPlan } from '../../../../services/training-plan.service';

@Component({
  selector: 'app-training-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './training-form.html',
  styleUrl: './training-form.scss'
})
export class TrainingForm implements OnInit {
  private fb = inject(FormBuilder);
  private trainingService = inject(TrainingService);
  private planService = inject(TrainingPlanService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  planId = signal<number>(0);
  plan = signal<TrainingPlan | null>(null);
  editId = signal<number | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  hasError = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    trainingType: [''],
    intensityLevel: [''],
    difficulty: [''],
    weekNumber: [null as number | null],
    dayOfWeek: [null as number | null],
    description: [''],
    benefit: [''],
    duration: [null as number | null],
    workPace: [''],
    recoveryPace: [''],
    intensityScore: [null as number | null],
    estimatedCalories: [null as number | null],
    estimatedDistanceMeters: [null as number | null],
    heroImageUrl: [''],
    steps: this.fb.array([]),
    prepTips: this.fb.array([])
  });

  get steps(): FormArray { return this.form.get('steps') as FormArray; }
  get prepTips(): FormArray { return this.form.get('prepTips') as FormArray; }
  get isEdit(): boolean { return this.editId() !== null; }

  ngOnInit(): void {
    const planIdParam = this.route.snapshot.paramMap.get('planId');
    const idParam = this.route.snapshot.paramMap.get('id');

    if (planIdParam) {
      const pid = parseInt(planIdParam, 10);
      this.planId.set(pid);
      this.planService.getById(pid).pipe(catchError(() => of(null)))
        .subscribe(p => this.plan.set(p));
    }

    if (idParam) {
      const id = parseInt(idParam, 10);
      this.editId.set(id);
      this.loadTraining(id);
    }
  }

  private loadTraining(id: number): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.trainingService.getTrainingById(id).pipe(
      catchError(() => { this.hasError.set(true); return of(null); }),
      finalize(() => this.isLoading.set(false))
    ).subscribe(t => {
      if (!t) return;
      this.form.patchValue({
        name: t.name,
        trainingType: t.trainingType ?? '',
        intensityLevel: t.intensityLevel ?? '',
        difficulty: t.difficulty ?? '',
        weekNumber: t.weekNumber ?? null,
        dayOfWeek: t.dayOfWeek ?? null,
        description: t.description ?? '',
        benefit: t.benefit ?? '',
        duration: t.duration ?? null,
        workPace: t.workPace ?? '',
        recoveryPace: t.recoveryPace ?? '',
        intensityScore: t.intensityScore ?? null,
        estimatedCalories: t.estimatedCalories ?? null,
        estimatedDistanceMeters: t.estimatedDistanceMeters ?? null,
        heroImageUrl: t.heroImageUrl ?? ''
      });
      (t.steps ?? []).forEach(s => this.steps.push(this.makeStepGroup(s)));
      (t.prepTips ?? []).forEach(p => this.prepTips.push(this.makeTipGroup(p)));
    });
  }

  private makeStepGroup(s?: Partial<{stepType:string;title:string;subtitle:string;durationMinutes:number;paceDisplay:string;icon:string;highlight:boolean;muted:boolean;repetitions:number}>): FormGroup {
    return this.fb.group({
      stepType: [s?.stepType ?? ''],
      title: [s?.title ?? '', Validators.required],
      subtitle: [s?.subtitle ?? ''],
      durationMinutes: [s?.durationMinutes ?? null],
      paceDisplay: [s?.paceDisplay ?? ''],
      icon: [s?.icon ?? ''],
      highlight: [s?.highlight ?? false],
      muted: [s?.muted ?? false],
      repetitions: [s?.repetitions ?? null]
    });
  }

  private makeTipGroup(p?: Partial<{title:string;icon:string;text:string}>): FormGroup {
    return this.fb.group({
      title: [p?.title ?? '', Validators.required],
      icon: [p?.icon ?? ''],
      text: [p?.text ?? '']
    });
  }

  addStep(): void { this.steps.push(this.makeStepGroup()); }
  removeStep(i: number): void { this.steps.removeAt(i); }
  moveStepUp(i: number): void { if (i === 0) return; const ctrl = this.steps.at(i); this.steps.removeAt(i); this.steps.insert(i - 1, ctrl); }
  moveStepDown(i: number): void { if (i >= this.steps.length - 1) return; const ctrl = this.steps.at(i); this.steps.removeAt(i); this.steps.insert(i + 1, ctrl); }

  addTip(): void { this.prepTips.push(this.makeTipGroup()); }
  removeTip(i: number): void { this.prepTips.removeAt(i); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const payload = this.buildPayload();
    this.isSaving.set(true);

    const op$ = this.isEdit
      ? this.trainingService.update(this.editId()!, payload)
      : this.trainingService.create(payload, this.planId());

    op$.pipe(
      catchError(() => { this.hasError.set(true); return of(null); }),
      finalize(() => this.isSaving.set(false))
    ).subscribe(result => {
      if (result) this.router.navigate(['/admin/plans', this.planId(), 'trainings']);
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/plans', this.planId(), 'trainings']);
  }

  private buildPayload(): any {
    const v = this.form.value;
    return {
      ...v,
      steps: (v.steps ?? []).map((s: any, i: number) => ({ ...s, sortOrder: i })),
      prepTips: (v.prepTips ?? []).map((p: any, i: number) => ({ ...p, sortOrder: i }))
    };
  }

  stepGroup(i: number): FormGroup { return this.steps.at(i) as FormGroup; }
  tipGroup(i: number): FormGroup { return this.prepTips.at(i) as FormGroup; }
}

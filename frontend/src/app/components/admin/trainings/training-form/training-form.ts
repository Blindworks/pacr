import { Component, OnInit, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { debounceTime } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';

import { TrainingService, TrainingStep, TrainingStepBlock } from '../../../../services/training.service';
import { TrainingPlanService, TrainingPlan } from '../../../../services/training-plan.service';

interface StepInit {
  stepType?: string;
  subtitle?: string;
  durationMinutes?: number;
  durationSeconds?: number;
  distanceMeters?: number;
  paceDisplay?: string;
  icon?: string;
  highlight?: boolean;
  muted?: boolean;
}

@Component({
  selector: 'app-training-form',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './training-form.html',
  styleUrl: './training-form.scss'
})
export class TrainingForm implements OnInit {
  private fb = inject(FormBuilder);
  private trainingService = inject(TrainingService);
  private planService = inject(TrainingPlanService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  private destroyRef = inject(DestroyRef);

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
    durationMinutes: [null as number | null],
    workPace: [''],
    recoveryPace: [''],
    intensityScore: [null as number | null],
    estimatedCalories: [null as number | null],
    estimatedDistanceMeters: [null as number | null],
    heroImageUrl: [''],
    items: this.fb.array([]),
    prepTips: this.fb.array([])
  });

  get items(): FormArray { return this.form.get('items') as FormArray; }
  get prepTips(): FormArray { return this.form.get('prepTips') as FormArray; }
  get isEdit(): boolean { return this.editId() !== null; }

  ngOnInit(): void {
    this.items.valueChanges.pipe(
      debounceTime(300),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.recalculateFromSteps());

    const planIdParam = this.route.snapshot.paramMap.get('planId');
    const idParam = this.route.snapshot.paramMap.get('id');

    if (planIdParam) {
      const pid = parseInt(planIdParam, 10);
      this.planId.set(pid);
      this.planService.getById(pid).subscribe({
        next: (p) => this.plan.set(p),
        error: () => this.plan.set(null)
      });
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
    this.trainingService.getTrainingById(id).subscribe({
      next: (t) => {
        if (!t) return;
        this.form.patchValue({
          name: t.name,
          trainingType: t.trainingType ?? '',
          intensityLevel: t.intensityLevel ?? '',
          difficulty: t.difficulty ?? '',
          weekNumber: t.weekNumber ?? null,
          dayOfWeek: this.toFormDayOfWeek(t.dayOfWeek),
          description: t.description ?? '',
          benefit: t.benefit ?? '',
          durationMinutes: t.durationMinutes ?? null,
          workPace: t.workPace ?? '',
          recoveryPace: t.recoveryPace ?? '',
          intensityScore: t.intensityScore ?? null,
          estimatedCalories: t.estimatedCalories ?? null,
          estimatedDistanceMeters: t.estimatedDistanceMeters ?? null,
          heroImageUrl: t.heroImageUrl ?? ''
        });
        this.items.clear();
        this.prepTips.clear();

        // Merge steps and blocks by sortOrder into a single ordered list
        type Tagged = { sortOrder: number; group: FormGroup };
        const tagged: Tagged[] = [];
        (t.steps ?? []).forEach(s => tagged.push({
          sortOrder: s.sortOrder ?? 0,
          group: this.makeStepItem(s)
        }));
        (t.blocks ?? []).forEach(b => tagged.push({
          sortOrder: b.sortOrder ?? 0,
          group: this.makeBlockItem(b)
        }));
        tagged.sort((a, b) => a.sortOrder - b.sortOrder);
        tagged.forEach(t => this.items.push(t.group));

        (t.prepTips ?? []).forEach(p => this.prepTips.push(this.makeTipGroup(p)));
      },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  private makeStepGroup(s?: StepInit): FormGroup {
    const durationSeconds = s?.durationSeconds ?? ((s?.durationMinutes ?? null) != null ? (s?.durationMinutes ?? 0) * 60 : null);
    return this.fb.group({
      stepType: [s?.stepType ?? '', Validators.required],
      subtitle: [s?.subtitle ?? ''],
      measurementType: [s?.distanceMeters != null ? 'distance' : 'duration', Validators.required],
      durationText: [this.formatDurationInput(durationSeconds)],
      distanceMeters: [s?.distanceMeters ?? null],
      paceDisplay: [s?.paceDisplay ?? ''],
      icon: [s?.icon ?? ''],
      highlight: [s?.highlight ?? false],
      muted: [s?.muted ?? false]
    });
  }

  private makeStepItem(s?: StepInit): FormGroup {
    return this.fb.group({
      type: ['step'],
      step: this.makeStepGroup(s)
    });
  }

  private makeBlockItem(b?: Partial<TrainingStepBlock>): FormGroup {
    const stepsArray = this.fb.array(
      (b?.steps ?? []).map(s => this.makeStepGroup(s))
    );
    return this.fb.group({
      type: ['block'],
      repeatCount: [b?.repeatCount ?? 2, [Validators.required, Validators.min(2)]],
      label: [b?.label ?? ''],
      steps: stepsArray
    });
  }

  private makeTipGroup(p?: Partial<{title:string;icon:string;text:string}>): FormGroup {
    return this.fb.group({
      title: [p?.title ?? '', Validators.required],
      icon: [p?.icon ?? ''],
      text: [p?.text ?? '']
    });
  }

  // ----- Top-level item operations -----
  addStep(): void { this.items.push(this.makeStepItem()); }
  addBlock(): void {
    const block = this.makeBlockItem({ repeatCount: 2, steps: [] });
    (block.get('steps') as FormArray).push(this.makeStepGroup());
    (block.get('steps') as FormArray).push(this.makeStepGroup());
    this.items.push(block);
  }

  removeItem(i: number): void { this.items.removeAt(i); }
  moveItemUp(i: number): void { if (i === 0) return; const ctrl = this.items.at(i); this.items.removeAt(i); this.items.insert(i - 1, ctrl); }
  moveItemDown(i: number): void { if (i >= this.items.length - 1) return; const ctrl = this.items.at(i); this.items.removeAt(i); this.items.insert(i + 1, ctrl); }

  copyItem(i: number): void {
    const v = this.items.at(i).value as any;
    if (v.type === 'step') {
      this.items.push(this.cloneStepItem(v.step));
    } else {
      const cloneSteps: StepInit[] = (v.steps ?? []).map((s: any) => this.stepValueToInit(s));
      const block = this.fb.group({
        type: ['block'],
        repeatCount: [v.repeatCount ?? 2, [Validators.required, Validators.min(2)]],
        label: [v.label ?? ''],
        steps: this.fb.array(cloneSteps.map(s => this.makeStepGroup(s)))
      });
      this.items.push(block);
    }
  }

  private cloneStepItem(s: any): FormGroup {
    return this.makeStepItem(this.stepValueToInit(s));
  }

  private stepValueToInit(s: any): StepInit {
    const seconds = s.measurementType === 'duration' ? this.parseDurationText(s.durationText) ?? undefined : undefined;
    return {
      stepType: s.stepType,
      subtitle: s.subtitle,
      distanceMeters: s.measurementType === 'distance' ? s.distanceMeters : undefined,
      durationSeconds: seconds,
      paceDisplay: s.paceDisplay,
      icon: s.icon,
      highlight: s.highlight,
      muted: s.muted
    };
  }

  // ----- Block-internal step operations -----
  blockSteps(itemIndex: number): FormArray {
    return this.items.at(itemIndex).get('steps') as FormArray;
  }

  addStepToBlock(itemIndex: number): void {
    this.blockSteps(itemIndex).push(this.makeStepGroup());
  }

  removeStepFromBlock(itemIndex: number, stepIndex: number): void {
    const arr = this.blockSteps(itemIndex);
    if (arr.length <= 1) return; // keep at least one step in the block
    arr.removeAt(stepIndex);
  }

  moveStepInBlockUp(itemIndex: number, stepIndex: number): void {
    if (stepIndex === 0) return;
    const arr = this.blockSteps(itemIndex);
    const ctrl = arr.at(stepIndex);
    arr.removeAt(stepIndex);
    arr.insert(stepIndex - 1, ctrl);
  }

  moveStepInBlockDown(itemIndex: number, stepIndex: number): void {
    const arr = this.blockSteps(itemIndex);
    if (stepIndex >= arr.length - 1) return;
    const ctrl = arr.at(stepIndex);
    arr.removeAt(stepIndex);
    arr.insert(stepIndex + 1, ctrl);
  }

  copyStepInBlock(itemIndex: number, stepIndex: number): void {
    const arr = this.blockSteps(itemIndex);
    const v = arr.at(stepIndex).value as any;
    arr.push(this.makeStepGroup(this.stepValueToInit(v)));
  }

  // ----- Prep tips -----
  addTip(): void { this.prepTips.push(this.makeTipGroup()); }
  removeTip(i: number): void { this.prepTips.removeAt(i); }

  save(): void {
    if (this.form.invalid || !this.validateAllSteps()) { this.form.markAllAsTouched(); return; }
    const payload = this.buildPayload();
    this.isSaving.set(true);

    const op$ = this.isEdit
      ? this.trainingService.update(this.editId()!, payload)
      : this.trainingService.create(payload, this.planId());

    op$.subscribe({
      next: (result) => { if (result) this.router.navigate(['/admin/plans', this.planId(), 'trainings']); },
      error: () => { this.hasError.set(true); this.isSaving.set(false); },
      complete: () => this.isSaving.set(false)
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/plans', this.planId(), 'trainings']);
  }

  private buildPayload(): any {
    const v = this.form.value;
    const steps: any[] = [];
    const blocks: any[] = [];

    (v.items ?? []).forEach((item: any, topIdx: number) => {
      if (item.type === 'step') {
        steps.push(this.serializeStep(item.step, topIdx));
      } else {
        blocks.push({
          sortOrder: topIdx,
          repeatCount: item.repeatCount,
          label: (item.label ?? '').trim() || null,
          steps: (item.steps ?? []).map((s: any, j: number) => {
            const serialized = this.serializeStep(s, 0);
            serialized.sortOrder = null;
            serialized.blockSortOrder = j;
            return serialized;
          })
        });
      }
    });

    return {
      ...v,
      items: undefined,
      dayOfWeek: this.toBackendDayOfWeek(v.dayOfWeek),
      steps,
      blocks,
      prepTips: (v.prepTips ?? []).map((p: any, i: number) => ({ ...p, sortOrder: i }))
    };
  }

  private serializeStep(s: any, sortOrder: number): any {
    const durationSeconds = s.measurementType === 'duration'
      ? this.parseDurationText(s.durationText)
      : null;
    const distanceMeters = s.measurementType === 'distance'
      ? this.parsePositiveInteger(s.distanceMeters)
      : null;

    return {
      stepType: s.stepType,
      title: this.formatStepTitle(s.stepType),
      subtitle: s.subtitle || null,
      durationMinutes: durationSeconds != null ? Math.max(1, Math.round(durationSeconds / 60)) : null,
      durationSeconds,
      distanceMeters,
      paceDisplay: this.normalizePaceDisplay(s.paceDisplay) ?? null,
      icon: s.icon || null,
      highlight: !!s.highlight,
      muted: !!s.muted,
      sortOrder
    };
  }

  itemGroup(i: number): FormGroup { return this.items.at(i) as FormGroup; }
  itemType(i: number): string { return this.itemGroup(i).get('type')?.value ?? 'step'; }
  stepInItem(i: number): FormGroup { return this.itemGroup(i).get('step') as FormGroup; }
  blockStepGroup(itemIndex: number, stepIndex: number): FormGroup {
    return this.blockSteps(itemIndex).at(stepIndex) as FormGroup;
  }
  tipGroup(i: number): FormGroup { return this.prepTips.at(i) as FormGroup; }

  protected onMeasurementTypeChange(group: FormGroup): void {
    const measurementType = group.get('measurementType')?.value;
    if (measurementType === 'duration') {
      group.get('distanceMeters')?.setValue(null);
      group.get('distanceMeters')?.setErrors(null);
    } else {
      group.get('durationText')?.setValue('');
      group.get('durationText')?.setErrors(null);
    }
  }

  protected onMaskedTimeInput(group: FormGroup, controlName: 'durationText' | 'paceDisplay'): void {
    const control = group.get(controlName);
    if (!control) return;

    const digits = String(control.value ?? '').replace(/\D/g, '').slice(0, 4);
    control.setValue(this.formatMaskedTime(digits), { emitEvent: false });

    if (controlName === 'durationText') {
      this.recalculateFromSteps();
    }
  }

  protected onMaskedTimeBlur(group: FormGroup, controlName: 'durationText' | 'paceDisplay'): void {
    const control = group.get(controlName);
    if (!control) return;

    const normalized = this.normalizePaceDisplay(control.value);
    if (normalized) {
      control.setValue(normalized, { emitEvent: false });
    }

    if (controlName === 'durationText') {
      this.recalculateFromSteps();
    }
  }

  private validateAllSteps(): boolean {
    let ok = true;
    this.items.controls.forEach(itemCtrl => {
      const g = itemCtrl as FormGroup;
      if (g.get('type')?.value === 'step') {
        if (!this.validateStepGroup(g.get('step') as FormGroup)) ok = false;
      } else {
        const steps = g.get('steps') as FormArray;
        if (steps.length === 0) ok = false;
        steps.controls.forEach(sc => { if (!this.validateStepGroup(sc as FormGroup)) ok = false; });
      }
    });
    return ok;
  }

  private validateStepGroup(group: FormGroup): boolean {
    let isValid = true;
    const measurementType = group.get('measurementType')?.value;
    const durationControl = group.get('durationText');
    const distanceControl = group.get('distanceMeters');

    durationControl?.setErrors(null);
    distanceControl?.setErrors(null);

    if (measurementType === 'duration') {
      if (this.parseDurationText(durationControl?.value) == null) {
        durationControl?.setErrors({ invalidDuration: true });
        isValid = false;
      }
    } else if (measurementType === 'distance') {
      if (this.parsePositiveInteger(distanceControl?.value) == null) {
        distanceControl?.setErrors({ invalidDistance: true });
        isValid = false;
      }
    } else {
      isValid = false;
    }

    const paceDisplayControl = group.get('paceDisplay');
    const paceDisplay = this.normalizePaceDisplay(paceDisplayControl?.value);
    paceDisplayControl?.setErrors(null);
    if (paceDisplayControl && paceDisplay == null && String(paceDisplayControl.value ?? '').trim() !== '') {
      paceDisplayControl.setErrors({ invalidPaceDisplay: true });
      isValid = false;
    }
    return isValid;
  }

  private kcalPerMinForStepType(stepType: string): number {
    switch (stepType) {
      case 'work':     return 10;
      case 'warmup':
      case 'cooldown': return 6;
      case 'recovery': return 4;
      case 'rest':     return 2;
      default:         return 7;
    }
  }

  private recalculateFromSteps(): void {
    const itemsValue = this.items.value as any[];
    if (!itemsValue || itemsValue.length === 0) return;

    let totalDurationSeconds = 0;
    let hasDurationSteps = false;
    let totalDistanceMeters = 0;
    let hasDistanceSteps = false;
    let totalCalories = 0;
    let hasCaloriesData = false;

    const accumulate = (step: any, multiplier: number) => {
      if (step.measurementType === 'duration') {
        const secs = this.parseDurationText(step.durationText);
        if (secs != null) {
          totalDurationSeconds += secs * multiplier;
          hasDurationSteps = true;
          totalCalories += (secs / 60) * this.kcalPerMinForStepType(step.stepType) * multiplier;
          hasCaloriesData = true;
        }
      } else if (step.measurementType === 'distance') {
        const dist = this.parsePositiveInteger(step.distanceMeters);
        if (dist != null) {
          totalDistanceMeters += dist * multiplier;
          hasDistanceSteps = true;
          totalCalories += (dist / 1000) * 70 * multiplier;
          hasCaloriesData = true;

          const paceSecs = this.parseDurationText(step.paceDisplay);
          if (paceSecs != null) {
            totalDurationSeconds += (dist / 1000) * paceSecs * multiplier;
            hasDurationSteps = true;
          }
        }
      }
    };

    for (const item of itemsValue) {
      if (item.type === 'step') {
        accumulate(item.step, 1);
      } else {
        const repeats = Math.max(2, Number(item.repeatCount) || 2);
        (item.steps ?? []).forEach((s: any) => accumulate(s, repeats));
      }
    }

    if (hasDurationSteps) {
      const minutes = Math.round(totalDurationSeconds / 60);
      this.form.get('durationMinutes')?.setValue(minutes > 0 ? minutes : null, { emitEvent: false });
    }

    if (hasDistanceSteps) {
      this.form.get('estimatedDistanceMeters')?.setValue(totalDistanceMeters > 0 ? totalDistanceMeters : null, { emitEvent: false });
    }

    if (hasCaloriesData) {
      this.form.get('estimatedCalories')?.setValue(Math.round(totalCalories) > 0 ? Math.round(totalCalories) : null, { emitEvent: false });
    }
  }

  private parseDurationText(value: unknown): number | null {
    if (typeof value !== 'string') {
      return null;
    }

    const trimmed = value.trim();
    const match = /^(\d{1,2}):([0-5]\d)$/.exec(trimmed);
    if (!match) {
      return null;
    }

    const minutes = Number.parseInt(match[1], 10);
    const seconds = Number.parseInt(match[2], 10);
    const totalSeconds = (minutes * 60) + seconds;
    return totalSeconds > 0 ? totalSeconds : null;
  }

  private formatDurationInput(value: number | null): string {
    if (value == null || value <= 0) {
      return '';
    }

    const minutes = Math.floor(value / 60);
    const seconds = value % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  private formatStepTitle(stepType: string | null | undefined): string {
    if (!stepType) {
      return 'Step';
    }

    return stepType
      .split(/[-_\s]+/)
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private parsePositiveInteger(value: unknown): number | null {
    const parsed = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  private normalizePaceDisplay(value: unknown): string | null {
    if (typeof value !== 'string') {
      return null;
    }

    const trimmed = value.trim();
    const match = /^(\d{1,2}):([0-5]\d)$/.exec(trimmed);
    if (!match) {
      return null;
    }

    const minutes = Number.parseInt(match[1], 10);
    const seconds = Number.parseInt(match[2], 10);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  private formatMaskedTime(digits: string): string {
    if (!digits) {
      return '';
    }

    if (digits.length <= 2) {
      return digits;
    }

    return `${digits.slice(0, 2)}:${digits.slice(2)}`;
  }

  private toFormDayOfWeek(value: number | string | null | undefined): number | null {
    if (typeof value === 'number') {
      return value >= 1 && value <= 7 ? value : null;
    }

    if (typeof value !== 'string') {
      return null;
    }

    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    const index = days.indexOf(value.trim().toUpperCase());
    return index >= 0 ? index + 1 : null;
  }

  private toBackendDayOfWeek(value: number | null | undefined): string | null {
    if (value == null || value < 1 || value > 7) {
      return null;
    }

    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    return days[value - 1];
  }
}

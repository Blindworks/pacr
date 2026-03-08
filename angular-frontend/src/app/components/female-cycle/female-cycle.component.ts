import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { FemaleCycleService } from '../../services/female-cycle.service';
import { CyclePhase, FemaleCycleEntry, FemaleCycleStatus } from '../../models/female-cycle.model';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';

@Component({
  selector: 'app-female-cycle',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    TranslatePipe
  ],
  templateUrl: './female-cycle.component.html',
  styleUrl: './female-cycle.component.scss'
})
export class FemaleCycleComponent implements OnInit {
  loading = true;
  isFemaleUser = false;
  userId: number | null = null;

  entries: FemaleCycleEntry[] = [];
  status: FemaleCycleStatus | null = null;

  cycleLength = 28;
  periodLength = 5;

  form: FormGroup;
  editId: string | null = null;

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private femaleCycleService: FemaleCycleService,
    private snackBar: MatSnackBar,
    private i18nService: I18nService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      date: [this.todayIso(), Validators.required],
      periodStarted: [false],
      flow: ['NONE', Validators.required],
      mood: ['OK', Validators.required],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.userId = this.authService.getCurrentUserId();
    if (!this.userId) {
      this.loading = false;
      return;
    }

    this.apiService.getMe().subscribe({
      next: user => {
        this.isFemaleUser = user.gender === 'FEMALE';
        if (this.isFemaleUser) {
          this.refreshData();
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  saveEntry(): void {
    if (!this.userId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.entries = this.femaleCycleService.saveEntry(this.userId, {
      date: value.date,
      periodStarted: !!value.periodStarted,
      flow: value.flow,
      mood: value.mood,
      notes: value.notes?.trim() || undefined
    }, this.editId ?? undefined);

    this.computeStatus();
    this.cancelEdit();
    this.showSnack('cycle.messages.saved');
  }

  editEntry(entry: FemaleCycleEntry): void {
    this.editId = entry.id;
    this.form.setValue({
      date: entry.date,
      periodStarted: entry.periodStarted,
      flow: entry.flow,
      mood: entry.mood,
      notes: entry.notes ?? ''
    });
  }

  deleteEntry(entry: FemaleCycleEntry): void {
    if (!this.userId) {
      return;
    }

    this.entries = this.femaleCycleService.deleteEntry(this.userId, entry.id);
    this.computeStatus();
    this.showSnack('cycle.messages.deleted');
  }

  cancelEdit(): void {
    this.editId = null;
    this.form.reset({
      date: this.todayIso(),
      periodStarted: false,
      flow: 'NONE',
      mood: 'OK',
      notes: ''
    });
  }

  onCycleConfigChange(): void {
    this.computeStatus();
  }

  getPhaseLabel(phase: CyclePhase | null): string {
    if (!phase) {
      return this.i18nService.t('cycle.phaseUnknown');
    }

    switch (phase) {
      case 'MENSTRUATION':
        return this.i18nService.t('cycle.phaseMenstruation');
      case 'FOLLICULAR':
        return this.i18nService.t('cycle.phaseFollicular');
      case 'OVULATION':
        return this.i18nService.t('cycle.phaseOvulation');
      case 'LUTEAL':
        return this.i18nService.t('cycle.phaseLuteal');
      default:
        return this.i18nService.t('cycle.phaseUnknown');
    }
  }

  getMoodLabel(mood: FemaleCycleEntry['mood']): string {
    const labels: Record<FemaleCycleEntry['mood'], string> = {
      VERY_BAD: this.i18nService.t('cycle.moodVeryBad'),
      BAD: this.i18nService.t('cycle.moodBad'),
      OK: this.i18nService.t('cycle.moodOk'),
      GOOD: this.i18nService.t('cycle.moodGood'),
      VERY_GOOD: this.i18nService.t('cycle.moodVeryGood')
    };

    return labels[mood];
  }

  getFlowLabel(flow: FemaleCycleEntry['flow']): string {
    const labels: Record<FemaleCycleEntry['flow'], string> = {
      NONE: this.i18nService.t('cycle.flowNone'),
      LIGHT: this.i18nService.t('cycle.flowLight'),
      MEDIUM: this.i18nService.t('cycle.flowMedium'),
      HEAVY: this.i18nService.t('cycle.flowHeavy')
    };

    return labels[flow];
  }

  formatDate(isoDate: string): string {
    const [year, month, day] = isoDate.split('-');
    return `${day}.${month}.${year}`;
  }

  private refreshData(): void {
    if (!this.userId) {
      return;
    }

    this.entries = this.femaleCycleService.getEntries(this.userId);
    this.computeStatus();
  }

  private computeStatus(): void {
    this.status = this.femaleCycleService.getCurrentStatus(this.entries, this.cycleLength, this.periodLength);
  }

  private todayIso(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private showSnack(key: string): void {
    this.snackBar.open(this.i18nService.t(key), this.i18nService.t('common.close'), { duration: 2500 });
  }
}

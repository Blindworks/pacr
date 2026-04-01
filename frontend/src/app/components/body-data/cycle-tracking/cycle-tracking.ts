import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CycleSettingsService } from '../../../services/cycle-settings.service';
import { CycleEntryService } from '../../../services/cycle-entry.service';

export type CyclePhase = 'menstrual' | 'follicular' | 'ovulation' | 'luteal';

export interface CycleDay {
  day: number | null;
  phase: CyclePhase | null;
  isToday: boolean;
}

@Component({
  selector: 'app-cycle-tracking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cycle-tracking.html',
  styleUrl: './cycle-tracking.scss'
})
export class CycleTracking implements OnInit {
  private readonly today = new Date();

  constructor(
    private router: Router,
    private cycleSettingsService: CycleSettingsService,
    private cycleEntryService: CycleEntryService
  ) {}

  goToLogSymptoms(): void {
    this.router.navigate(['/body-data/log-symptoms']);
  }

  goToCycleSettings(): void {
    this.router.navigate(['/body-data/cycle-settings']);
  }

  currentPhase: CyclePhase = 'follicular';
  currentDay = 8;
  cycleLength = 28;
  periodDuration = 5;
  daysRemaining = 4;
  nextPhase: CyclePhase = 'ovulation';
  showNewCyclePrompt = false;

  currentMonth = '';
  calendarDays: CycleDay[] = [];

  readonly phaseLabels: Record<CyclePhase, string> = {
    menstrual: 'Menstrual',
    follicular: 'Follicular',
    ovulation: 'Ovulation',
    luteal: 'Luteal',
  };

  readonly phaseInsights: Record<CyclePhase, { performance: string; fueling: string; intensity: number; label: string }> = {
    follicular: {
      performance: 'High energy levels. Estrogen is rising, improving muscle strength and recovery speed. Great window for PR attempts or speedwork.',
      fueling: 'Higher carbohydrate tolerance. Focus on complex carbs for sustained intensity.',
      intensity: 90,
      label: 'Peak Phase',
    },
    ovulation: {
      performance: 'Peak strength and coordination. Testosterone peaks alongside estrogen — ideal for power training and race efforts.',
      fueling: 'Moderate carbs and protein. Hydration is key as body temperature rises slightly.',
      intensity: 95,
      label: 'Ovulation Peak',
    },
    luteal: {
      performance: 'Energy may dip in late luteal phase. Progesterone rises, increasing perceived effort. Favour steady-state and tempo runs.',
      fueling: 'Increased caloric needs. Focus on iron-rich foods and complex carbs to support elevated metabolism.',
      intensity: 70,
      label: 'Moderate Phase',
    },
    menstrual: {
      performance: 'Rest and recovery priority. Low hormone levels — listen to your body. Light movement supports wellbeing.',
      fueling: 'Iron and magnesium intake is important. Anti-inflammatory foods help manage discomfort.',
      intensity: 40,
      label: 'Recovery Phase',
    },
  };

  get insight() {
    return this.phaseInsights[this.currentPhase];
  }

  get intensityDashoffset(): number {
    const circumference = 364;
    return circumference - (this.insight.intensity / 100) * circumference;
  }

  ngOnInit(): void {
    this.currentMonth = new Intl.DateTimeFormat('en-US', {
      month: 'long',
      year: 'numeric',
    }).format(this.today);

    this.cycleSettingsService.getStatus().subscribe({
      next: (status) => {
        this.currentPhase = status.currentPhase as CyclePhase;
        this.currentDay = status.currentDay;
        this.cycleLength = status.cycleLength;
        this.periodDuration = status.periodDuration;
        this.daysRemaining = status.daysRemainingInPhase;
        this.nextPhase = status.nextPhase as CyclePhase;
        this.showNewCyclePrompt = status.shouldShowNewCyclePrompt;
        this.buildCalendar();
      },
      error: () => {
        // No settings yet — use defaults and show no prompt
        this.buildCalendar();
      }
    });
  }

  confirmNewCycle(): void {
    const todayStr = new Intl.DateTimeFormat('sv-SE').format(this.today);
    this.cycleEntryService.create({
      entryDate: todayStr,
      flowIntensity: 'MEDIUM',
    }).subscribe({
      next: () => {
        this.showNewCyclePrompt = false;
        // Reload status to reflect the new cycle
        this.cycleSettingsService.getStatus().subscribe({
          next: (status) => {
            this.currentPhase = status.currentPhase as CyclePhase;
            this.currentDay = status.currentDay;
            this.cycleLength = status.cycleLength;
            this.periodDuration = status.periodDuration;
            this.daysRemaining = status.daysRemainingInPhase;
            this.nextPhase = status.nextPhase as CyclePhase;
            this.buildCalendar();
          },
          error: () => {}
        });
      },
      error: () => {}
    });
  }

  dismissNewCyclePrompt(): void {
    this.showNewCyclePrompt = false;
    sessionStorage.setItem('newCyclePromptDismissed', new Intl.DateTimeFormat('sv-SE').format(this.today));
  }

  private buildCalendar(): void {
    this.calendarDays = [];

    const year = this.today.getFullYear();
    const month = this.today.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDayOfMonth = new Date(year, month, 1);
    const leadingEmptyDays = (firstDayOfMonth.getDay() + 6) % 7;

    for (let i = 0; i < leadingEmptyDays; i++) {
      this.calendarDays.push({
        day: null,
        phase: null,
        isToday: false,
      });
    }

    for (let day = 1; day <= daysInMonth; day++) {
      const dayOffset = day - this.today.getDate();

      this.calendarDays.push({
        day,
        phase: this.getPhaseForCycleDay(this.getCycleDayForOffset(dayOffset)),
        isToday: day === this.today.getDate(),
      });
    }
  }

  phaseClass(phase: CyclePhase | null): string {
    if (!phase) {
      return '';
    }

    return `phase-${phase}`;
  }

  private getCycleDayForOffset(dayOffset: number): number {
    return ((this.currentDay - 1 + dayOffset) % this.cycleLength + this.cycleLength) % this.cycleLength + 1;
  }

  get phaseProgressPercent(): number {
    const phaseEndDay = this.phaseEndDay(this.currentPhase);
    const phaseStartDay = this.phaseStartDay(this.currentPhase);
    const totalPhaseDays = phaseEndDay - phaseStartDay + 1;
    const elapsed = totalPhaseDays - this.daysRemaining;
    return Math.min(100, Math.max(0, (elapsed / totalPhaseDays) * 100));
  }

  private phaseStartDay(phase: CyclePhase): number {
    switch (phase) {
      case 'menstrual': return 1;
      case 'follicular': return this.periodDuration + 1;
      case 'ovulation': return Math.floor(this.cycleLength / 2) - 1;
      case 'luteal': return Math.floor(this.cycleLength / 2) + 1;
    }
  }

  private phaseEndDay(phase: CyclePhase): number {
    switch (phase) {
      case 'menstrual': return this.periodDuration;
      case 'follicular': return Math.floor(this.cycleLength / 2) - 2;
      case 'ovulation': return Math.floor(this.cycleLength / 2);
      case 'luteal': return this.cycleLength;
    }
  }

  private getPhaseForCycleDay(cycleDay: number): CyclePhase {
    if (cycleDay <= this.periodDuration) {
      return 'menstrual';
    }

    if (cycleDay <= this.cycleLength / 2 - 2) {
      return 'follicular';
    }

    if (cycleDay <= this.cycleLength / 2) {
      return 'ovulation';
    }

    return 'luteal';
  }
}

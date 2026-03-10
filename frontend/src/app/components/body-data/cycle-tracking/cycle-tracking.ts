import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

export type CyclePhase = 'menstrual' | 'follicular' | 'ovulation' | 'luteal';

export interface CycleDay {
  day: number;
  phase: CyclePhase;
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
  constructor(private router: Router) {}

  goToLogSymptoms(): void {
    this.router.navigate(['/body-data/log-symptoms']);
  }

  currentPhase: CyclePhase = 'follicular';
  currentDay = 8;
  cycleLength = 28;
  daysRemaining = 4;
  nextPhase: CyclePhase = 'ovulation';

  currentMonth = 'March 2026';

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
    this.buildCalendar();
  }

  private buildCalendar(): void {
    const phases: CyclePhase[] = [
      'menstrual', 'menstrual', 'menstrual',
      'follicular', 'follicular', 'follicular', 'follicular',
      'follicular', 'follicular', 'follicular', 'follicular',
      'ovulation', 'ovulation',
      'luteal', 'luteal', 'luteal', 'luteal', 'luteal',
      'luteal', 'luteal',
    ];

    for (let i = 0; i < 21; i++) {
      this.calendarDays.push({
        day: i + 1,
        phase: phases[i] ?? 'luteal',
        isToday: i + 1 === this.currentDay,
      });
    }
  }

  phaseClass(phase: CyclePhase): string {
    return `phase-${phase}`;
  }
}

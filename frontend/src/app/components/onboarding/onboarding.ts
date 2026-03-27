import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService, UserProfile } from '../../services/user.service';
import { TrainingPlanService, TrainingPlan } from '../../services/training-plan.service';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './onboarding.html',
  styleUrl: './onboarding.scss'
})
export class Onboarding implements OnInit {
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly planService = inject(TrainingPlanService);

  protected readonly currentStep = signal(1);
  protected readonly totalSteps = 5;
  protected readonly saving = signal(false);

  // Step 1: Basics
  protected readonly gender = signal<string | null>(null);
  protected readonly age = signal<string>('');
  protected readonly height = signal<string>('');
  protected readonly weight = signal<string>('');

  // Step 2: Intelligence
  protected readonly cycleEnabled = signal(false);
  protected readonly bioWeatherEnabled = signal(false);
  protected readonly asthmaEnabled = signal(false);

  // Step 3: Goal Assessment
  protected readonly targetDistance = signal<string | null>(null);
  protected readonly weeklyVolume = signal<string | null>(null);

  // Step 4: Plan Selection
  protected readonly availablePlans = signal<TrainingPlan[]>([]);
  protected readonly selectedPlan = signal<TrainingPlan | null>(null);
  protected readonly startDateMode = signal<'immediate' | 'custom'>('immediate');
  protected readonly customStartDate = signal('');

  // Step 5: Final
  protected readonly setupComplete = signal(false);
  protected readonly competitionName = signal('');

  protected readonly progress = computed(() => {
    return ((this.currentStep() - 1) / (this.totalSteps - 1)) * 100;
  });

  protected readonly phaseLabels: Record<number, string> = {
    1: 'PHASE 01 // INITIALIZATION',
    2: 'PHASE 01.5 // INTELLIGENCE OPT-IN',
    3: 'PHASE 02 // GOAL DEFINITION',
    4: 'PHASE 03 // PLAN CALIBRATION',
    5: 'PHASE 04 // FINALIZATION'
  };

  private userId = 0;

  ngOnInit(): void {
    const user = this.userService.currentUser();
    if (user) {
      this.prefillFromUser(user);
    } else {
      this.userService.getMe().subscribe(u => this.prefillFromUser(u));
    }
  }

  private prefillFromUser(user: UserProfile): void {
    this.userId = user.id;
    if (user.gender) this.gender.set(user.gender);
    if (user.heightCm) this.height.set(String(user.heightCm));
    if (user.weightKg) this.weight.set(String(user.weightKg));
    if (user.dateOfBirth) {
      const birthYear = new Date(user.dateOfBirth).getFullYear();
      const currentYear = new Date().getFullYear();
      this.age.set(String(currentYear - birthYear));
    }
    this.cycleEnabled.set(user.cycleTrackingEnabled ?? false);
    this.asthmaEnabled.set(user.asthmaTrackingEnabled ?? false);
    if (user.targetDistance) this.targetDistance.set(user.targetDistance);
    if (user.weeklyVolumeKm) this.weeklyVolume.set(user.weeklyVolumeKm);
  }

  protected nextStep(): void {
    if (this.saving()) return;

    const step = this.currentStep();
    this.saving.set(true);

    if (step === 1) {
      this.saveBasics();
    } else if (step === 2) {
      this.saveIntelligence();
    } else if (step === 3) {
      this.saveGoals();
    } else if (step === 4) {
      this.savePlanSelection();
    } else if (step === 5) {
      this.finishOnboarding();
      return;
    }
  }

  protected prevStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.update(s => s - 1);
    }
  }

  private saveBasics(): void {
    const dateOfBirth = this.age()
      ? this.calculateDateOfBirth(parseInt(this.age(), 10))
      : null;

    this.userService.updateUser(this.userId, {
      gender: this.gender(),
      dateOfBirth: dateOfBirth,
      heightCm: this.height() ? parseInt(this.height(), 10) : null,
      weightKg: this.weight() ? parseFloat(this.weight()) : null
    }).subscribe({
      next: () => this.advanceStep(),
      error: () => this.advanceStep()
    });
  }

  private saveIntelligence(): void {
    this.userService.updateUser(this.userId, {
      cycleTrackingEnabled: this.cycleEnabled(),
      asthmaTrackingEnabled: this.asthmaEnabled()
    }).subscribe({
      next: () => {
        this.loadPlans();
        this.advanceStep();
      },
      error: () => {
        this.loadPlans();
        this.advanceStep();
      }
    });
  }

  private saveGoals(): void {
    this.userService.updateUser(this.userId, {
      targetDistance: this.targetDistance(),
      weeklyVolumeKm: this.weeklyVolume()
    }).subscribe({
      next: () => {
        this.loadPlans();
        this.advanceStep();
      },
      error: () => this.advanceStep()
    });
  }

  private savePlanSelection(): void {
    const plan = this.selectedPlan();
    if (!plan) {
      this.advanceStep();
      return;
    }

    const startDate = this.startDateMode() === 'custom' && this.customStartDate()
      ? this.customStartDate()
      : this.getNextMonday();

    this.userService.setupOnboardingPlan(plan.id, startDate).subscribe({
      next: (result: any) => {
        this.setupComplete.set(true);
        if (result?.name) this.competitionName.set(result.name);
        this.advanceStep();
      },
      error: () => this.advanceStep()
    });
  }

  private finishOnboarding(): void {
    this.userService.completeOnboarding().subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/']);
      },
      error: () => {
        this.saving.set(false);
        this.router.navigate(['/']);
      }
    });
  }

  private advanceStep(): void {
    this.saving.set(false);
    this.currentStep.update(s => Math.min(s + 1, this.totalSteps));
  }

  private loadPlans(): void {
    this.planService.getTemplates().subscribe({
      next: plans => {
        const filtered = this.filterPlansByGoal(plans);
        this.availablePlans.set(filtered.length > 0 ? filtered : plans);
        if (filtered.length > 0) {
          this.selectedPlan.set(filtered[0]);
        } else if (plans.length > 0) {
          this.selectedPlan.set(plans[0]);
        }
      },
      error: () => this.availablePlans.set([])
    });
  }

  private filterPlansByGoal(plans: TrainingPlan[]): TrainingPlan[] {
    const target = this.targetDistance();
    if (!target) return plans;

    const typeMap: Record<string, string[]> = {
      '5K': ['FIVE_K', '5K'],
      '10K': ['TEN_K', '10K'],
      'HALF_MARATHON': ['HALF_MARATHON', 'HALBMARATHON'],
      'MARATHON': ['MARATHON'],
      'FUN': []
    };

    const types = typeMap[target] ?? [];
    if (types.length === 0) return plans;

    return plans.filter(p =>
      p.competitionType && types.some(t =>
        p.competitionType!.toUpperCase().includes(t)
      )
    );
  }

  private calculateDateOfBirth(age: number): string {
    const now = new Date();
    const birthYear = now.getFullYear() - age;
    return `${birthYear}-01-01`;
  }

  private getNextMonday(): string {
    const today = new Date();
    const day = today.getDay();
    const daysUntilMonday = day === 0 ? 1 : 8 - day;
    const nextMonday = new Date(today);
    nextMonday.setDate(today.getDate() + daysUntilMonday);
    return nextMonday.toISOString().split('T')[0];
  }

  protected selectGender(g: string): void {
    this.gender.set(g);
  }

  protected toggleIntel(feature: 'cycle' | 'bioWeather' | 'asthma'): void {
    if (feature === 'cycle') this.cycleEnabled.update(v => !v);
    else if (feature === 'bioWeather') this.bioWeatherEnabled.update(v => !v);
    else this.asthmaEnabled.update(v => !v);
  }

  protected selectDistance(d: string): void {
    this.targetDistance.set(d);
  }

  protected selectVolume(v: string): void {
    this.weeklyVolume.set(v);
  }

  protected selectPlan(plan: TrainingPlan): void {
    this.selectedPlan.set(plan);
  }

  protected setStartMode(mode: 'immediate' | 'custom'): void {
    this.startDateMode.set(mode);
  }

  protected getDistanceLabel(d: string): string {
    const labels: Record<string, string> = {
      '5K': '5K',
      '10K': '10K',
      'HALF_MARATHON': 'Half Marathon',
      'MARATHON': 'Marathon',
      'FUN': 'Just for Fun/Health'
    };
    return labels[d] ?? d;
  }

  protected getDistanceSub(d: string): string {
    const subs: Record<string, string> = {
      '5K': 'Foundation Run',
      '10K': 'Endurance Build',
      'HALF_MARATHON': '21.1 KM Precision',
      'MARATHON': '42.2 KM Mastery',
      'FUN': 'Consistent Movement'
    };
    return subs[d] ?? '';
  }

  protected getVolumeLabel(v: string): string {
    const labels: Record<string, string> = {
      '0_10': '0-10km',
      '10_30': '10-30km',
      '20_40': '20-40km',
      '40_60': '40-60km',
      '60_PLUS': '60km+'
    };
    return labels[v] ?? v;
  }

  protected goToDashboard(): void {
    this.finishOnboarding();
  }
}

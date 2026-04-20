import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { TrainingService, Training } from '../../../../services/training.service';
import { TrainingPlanService, TrainingPlan } from '../../../../services/training-plan.service';

@Component({
  selector: 'app-training-list',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './training-list.html',
  styleUrl: './training-list.scss'
})
export class TrainingList implements OnInit {
  private trainingService = inject(TrainingService);
  private planService = inject(TrainingPlanService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  planId = signal<number>(0);
  plan = signal<TrainingPlan | null>(null);
  trainings = signal<Training[]>([]);
  isLoading = signal(false);
  hasError = signal(false);
  confirmDeleteId = signal<number | null>(null);

  ngOnInit(): void {
    const id = parseInt(this.route.snapshot.paramMap.get('planId')!, 10);
    this.planId.set(id);
    this.loadPlan(id);
    this.loadTrainings(id);
  }

  private loadPlan(id: number): void {
    this.planService.getById(id).subscribe({
      next: (p) => this.plan.set(p),
      error: () => this.plan.set(null)
    });
  }

  loadTrainings(id: number = this.planId()): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.trainingService.getByPlan(id).subscribe({
      next: (data) => {
        this.trainings.set(data);
        this.scheduleRestoreScroll();
      },
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  private scrollKey(): string {
    return `admin-trainings-scroll-${this.planId()}`;
  }

  private scrollContainers(): HTMLElement[] {
    const containers: HTMLElement[] = [];
    const admin = document.querySelector('.admin-content') as HTMLElement | null;
    if (admin) containers.push(admin);
    containers.push(document.scrollingElement as HTMLElement || document.documentElement);
    return containers;
  }

  private getCurrentScroll(): number {
    for (const el of this.scrollContainers()) {
      if (el && el.scrollTop > 0) return el.scrollTop;
    }
    return 0;
  }

  private saveScroll(): void {
    sessionStorage.setItem(this.scrollKey(), String(this.getCurrentScroll()));
  }

  private scheduleRestoreScroll(): void {
    const stored = sessionStorage.getItem(this.scrollKey());
    if (stored == null) return;
    const target = Number(stored) || 0;
    if (target <= 0) return;

    let attempts = 0;
    const tryRestore = () => {
      attempts++;
      let applied = false;
      for (const el of this.scrollContainers()) {
        if (el && el.scrollHeight - el.clientHeight >= target) {
          el.scrollTop = target;
          if (el.scrollTop >= target - 1) applied = true;
        }
      }
      if (!applied && attempts < 20) {
        requestAnimationFrame(tryRestore);
      } else {
        sessionStorage.removeItem(this.scrollKey());
      }
    };
    requestAnimationFrame(tryRestore);
  }

  navigateNew(): void {
    this.saveScroll();
    this.router.navigate(['/admin/plans', this.planId(), 'trainings', 'new']);
  }

  navigateEdit(id: number): void {
    this.saveScroll();
    this.router.navigate(['/admin/plans', this.planId(), 'trainings', id, 'edit']);
  }

  backToPlans(): void {
    this.router.navigate(['/admin/plans']);
  }

  requestDelete(id: number): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(id: number): void {
    this.trainingService.delete(id).subscribe({
      next: () => { this.confirmDeleteId.set(null); this.loadTrainings(); },
      error: () => this.hasError.set(true)
    });
  }

  intensityClass(level?: string): string {
    switch (level) {
      case 'high': return 'pill--high';
      case 'medium': return 'pill--medium';
      case 'low': return 'pill--low';
      case 'recovery': return 'pill--recovery';
      default: return 'pill--default';
    }
  }

  distanceLabel(meters?: number): string {
    if (meters == null || meters <= 0) return '—';
    if (meters >= 1000) {
      const km = meters / 1000;
      const formatted = km % 1 === 0 ? km.toFixed(0) : km.toFixed(2).replace(/\.?0+$/, '');
      return `${formatted} km`;
    }
    return `${meters} m`;
  }

  dayLabel(day?: number | string): string {
    if (typeof day === 'string') {
      const enumMap: Record<string, string> = {
        MONDAY: 'Mon',
        TUESDAY: 'Tue',
        WEDNESDAY: 'Wed',
        THURSDAY: 'Thu',
        FRIDAY: 'Fri',
        SATURDAY: 'Sat',
        SUNDAY: 'Sun'
      };
      return enumMap[day] ?? day;
    }

    const days = ['', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    return day != null ? (days[day] ?? `D${day}`) : '—';
  }
}

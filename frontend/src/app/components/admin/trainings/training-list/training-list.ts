import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { TrainingService, Training } from '../../../../services/training.service';
import { TrainingPlanService, TrainingPlan } from '../../../../services/training-plan.service';

@Component({
  selector: 'app-training-list',
  standalone: true,
  imports: [],
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
    this.planService.getById(id).pipe(
      catchError(() => of(null))
    ).subscribe(p => this.plan.set(p));
  }

  loadTrainings(id: number = this.planId()): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.trainingService.getByPlan(id).pipe(
      catchError(() => { this.hasError.set(true); return of([]); }),
      finalize(() => this.isLoading.set(false))
    ).subscribe(data => this.trainings.set(data));
  }

  navigateNew(): void {
    this.router.navigate(['/admin/plans', this.planId(), 'trainings', 'new']);
  }

  navigateEdit(id: number): void {
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
    this.trainingService.delete(id).pipe(
      catchError(() => { this.hasError.set(true); return of(void 0); })
    ).subscribe(() => {
      this.confirmDeleteId.set(null);
      this.loadTrainings();
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

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { TrainingPlanService, TrainingPlan } from '../../../../services/training-plan.service';

@Component({
  selector: 'app-plan-list',
  standalone: true,
  imports: [],
  templateUrl: './plan-list.html',
  styleUrl: './plan-list.scss'
})
export class PlanList implements OnInit {
  private planService = inject(TrainingPlanService);
  private router = inject(Router);

  plans = signal<TrainingPlan[]>([]);
  isLoading = signal(false);
  hasError = signal(false);
  confirmDeleteId = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.planService.getAll().pipe(
      catchError(() => { this.hasError.set(true); return of([]); }),
      finalize(() => this.isLoading.set(false))
    ).subscribe(data => this.plans.set(data));
  }

  navigateNew(): void {
    this.router.navigate(['/admin/plans/new']);
  }

  navigateEdit(id: number): void {
    this.router.navigate(['/admin/plans', id, 'edit']);
  }

  navigateTrainings(id: number): void {
    this.router.navigate(['/admin/plans', id, 'trainings']);
  }

  requestDelete(id: number): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(id: number): void {
    this.planService.delete(id).pipe(
      catchError(() => { this.hasError.set(true); return of(void 0); })
    ).subscribe(() => {
      this.confirmDeleteId.set(null);
      this.load();
    });
  }

  competitionTypeLabel(type?: string): string {
    if (!type) return '—';
    return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
}

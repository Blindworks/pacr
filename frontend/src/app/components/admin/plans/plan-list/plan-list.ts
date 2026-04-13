import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { TrainingPlanService, TrainingPlan } from '../../../../services/training-plan.service';

@Component({
  selector: 'app-plan-list',
  standalone: true,
  imports: [TranslateModule],
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

  // Upload state
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  selectedFile = signal<File | null>(null);
  isUploading = signal(false);
  uploadSuccess = signal(false);
  uploadError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.planService.getAll().subscribe({
      next: (data) => this.plans.set(data),
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
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
    this.planService.delete(id).subscribe({
      next: () => { this.confirmDeleteId.set(null); this.load(); },
      error: () => this.hasError.set(true)
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
    this.uploadSuccess.set(false);
    this.uploadError.set(null);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.isUploading.set(true);
    this.uploadError.set(null);
    this.uploadSuccess.set(false);

    this.planService.uploadTemplate(file).subscribe({
      next: () => {
        this.uploadSuccess.set(true);
        this.selectedFile.set(null);
        if (this.fileInput) this.fileInput.nativeElement.value = '';
        this.isUploading.set(false);
        this.load();
      },
      error: () => {
        this.uploadError.set('ADMIN.PLANS.UPLOAD_ERROR');
        this.isUploading.set(false);
      }
    });
  }

  competitionTypeLabel(type?: string): string {
    if (!type) return '—';
    return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { CompetitionService, Competition } from '../../../../services/competition.service';

@Component({
  selector: 'app-competition-list',
  standalone: true,
  imports: [TranslateModule, FormsModule],
  templateUrl: './competition-list.html',
  styleUrl: './competition-list.scss'
})
export class CompetitionList implements OnInit {
  private service = inject(CompetitionService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  competitions = signal<Competition[]>([]);
  isLoading = signal(false);
  hasError = signal(false);

  confirmDeleteComp = signal<Competition | null>(null);
  confirmInput = signal('');
  deleteInProgress = signal(false);
  deleteError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.service.getAll().subscribe({
      next: (data) => this.competitions.set(data),
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  navigateNew(): void {
    this.router.navigate(['/admin/competitions/new']);
  }

  navigateEdit(id: number): void {
    this.router.navigate(['/admin/competitions', id, 'edit']);
  }

  requestDelete(comp: Competition): void {
    this.confirmDeleteComp.set(comp);
    this.confirmInput.set('');
    this.deleteError.set(null);
  }

  cancelDelete(): void {
    if (this.deleteInProgress()) return;
    this.confirmDeleteComp.set(null);
    this.confirmInput.set('');
    this.deleteError.set(null);
  }

  canConfirmDelete(): boolean {
    const target = this.confirmDeleteComp();
    return !!target && this.confirmInput() === target.name && !this.deleteInProgress();
  }

  confirmDelete(): void {
    const target = this.confirmDeleteComp();
    if (!target || !this.canConfirmDelete()) return;
    this.deleteInProgress.set(true);
    this.deleteError.set(null);
    this.service.delete(target.id).subscribe({
      next: () => {
        this.deleteInProgress.set(false);
        this.confirmDeleteComp.set(null);
        this.confirmInput.set('');
        this.load();
      },
      error: (err) => {
        this.deleteInProgress.set(false);
        this.deleteError.set(err?.error?.message ?? this.translate.instant('ADMIN.COMP_DELETE_FAILED'));
      }
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  private readonly typeLabels: Record<string, string> = {
    FIVE_K: '5K', TEN_K: '10K', HALF_MARATHON: 'Halbmarathon', MARATHON: 'Marathon',
    FIFTY_K: '50K', HUNDRED_K: '100K', BACKYARD_ULTRA: 'Backyard Ultra',
    CATCHER_CAR: 'Catcher Car', OTHER: 'Sonstige'
  };

  formatType(type: string): string {
    return this.typeLabels[type] ?? type;
  }
}

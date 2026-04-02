import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { CompetitionService, Competition } from '../../../../services/competition.service';

@Component({
  selector: 'app-competition-list',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './competition-list.html',
  styleUrl: './competition-list.scss'
})
export class CompetitionList implements OnInit {
  private service = inject(CompetitionService);
  private router = inject(Router);

  competitions = signal<Competition[]>([]);
  isLoading = signal(false);
  hasError = signal(false);
  confirmDeleteId = signal<number | null>(null);

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

  requestDelete(id: number): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(id: number): void {
    this.service.delete(id).subscribe({
      next: () => { this.confirmDeleteId.set(null); this.load(); },
      error: () => this.hasError.set(true)
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}

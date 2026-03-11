import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { CompetitionService, Competition } from '../../../../services/competition.service';

@Component({
  selector: 'app-competition-list',
  standalone: true,
  imports: [],
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
    this.service.getAll().pipe(
      catchError(() => { this.hasError.set(true); return of([]); }),
      finalize(() => this.isLoading.set(false))
    ).subscribe(data => this.competitions.set(data));
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
    this.service.delete(id).pipe(
      catchError(() => { this.hasError.set(true); return of(void 0); })
    ).subscribe(() => {
      this.confirmDeleteId.set(null);
      this.load();
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}

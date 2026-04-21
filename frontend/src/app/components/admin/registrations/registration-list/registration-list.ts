import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { AdminRegistration, AdminRegistrationService } from '../../../../services/admin-registration.service';

type ConfirmTarget =
  | { kind: 'registration'; registration: AdminRegistration }
  | { kind: 'competition'; registration: AdminRegistration };

@Component({
  selector: 'app-admin-registration-list',
  standalone: true,
  imports: [TranslateModule, FormsModule],
  templateUrl: './registration-list.html',
  styleUrl: './registration-list.scss'
})
export class RegistrationList implements OnInit {
  private service = inject(AdminRegistrationService);
  private translate = inject(TranslateService);

  registrations = signal<AdminRegistration[]>([]);
  isLoading = signal(false);
  hasError = signal(false);
  searchTerm = signal('');

  confirmTarget = signal<ConfirmTarget | null>(null);
  confirmInput = signal('');
  deleteInProgress = signal(false);
  deleteError = signal<string | null>(null);

  filtered = computed<AdminRegistration[]>(() => {
    const q = this.searchTerm().trim().toLowerCase();
    if (!q) return this.registrations();
    return this.registrations().filter(r =>
      r.userEmail?.toLowerCase().includes(q) ||
      r.userDisplayName?.toLowerCase().includes(q) ||
      r.competitionName?.toLowerCase().includes(q) ||
      r.trainingPlanName?.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.service.getAll().subscribe({
      next: data => this.registrations.set(data),
      error: () => { this.hasError.set(true); this.isLoading.set(false); },
      complete: () => this.isLoading.set(false)
    });
  }

  requestDeleteRegistration(reg: AdminRegistration): void {
    this.confirmTarget.set({ kind: 'registration', registration: reg });
    this.confirmInput.set('');
    this.deleteError.set(null);
  }

  requestDeleteCompetition(reg: AdminRegistration): void {
    this.confirmTarget.set({ kind: 'competition', registration: reg });
    this.confirmInput.set('');
    this.deleteError.set(null);
  }

  cancelDelete(): void {
    if (this.deleteInProgress()) return;
    this.confirmTarget.set(null);
    this.confirmInput.set('');
    this.deleteError.set(null);
  }

  confirmPhrase(): string {
    const target = this.confirmTarget();
    if (!target) return '';
    return target.kind === 'competition' ? target.registration.competitionName : 'DELETE';
  }

  canConfirmDelete(): boolean {
    const target = this.confirmTarget();
    if (!target || this.deleteInProgress()) return false;
    return this.confirmInput() === this.confirmPhrase();
  }

  confirmDelete(): void {
    const target = this.confirmTarget();
    if (!target || !this.canConfirmDelete()) return;
    this.deleteInProgress.set(true);
    this.deleteError.set(null);

    const req = target.kind === 'registration'
      ? this.service.deleteRegistration(target.registration.id)
      : this.service.deleteCompetition(target.registration.competitionId);

    req.subscribe({
      next: () => {
        this.deleteInProgress.set(false);
        this.confirmTarget.set(null);
        this.confirmInput.set('');
        this.load();
      },
      error: (err) => {
        this.deleteInProgress.set(false);
        this.deleteError.set(err?.error?.message ?? this.translate.instant('ADMIN.REGISTRATIONS.DELETE_FAILED'));
      }
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDateTime(dateStr?: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' }) +
           ' ' + d.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
  }
}

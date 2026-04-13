import { Component, OnInit, inject, signal, computed } from '@angular/core';
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

  // Filter state
  showPastRaces = signal(false);
  activeTypeFilter = signal<string>('ALL');
  activeMonthFilter = signal<number | null>(null);
  activeYearFilter = signal<number | null>(null);

  // Type filter options
  readonly typeFilterOptions: { key: string; label: string }[] = [
    { key: 'ALL', label: 'COMMON.ALL' },
    { key: '5K', label: '5K' },
    { key: '10K', label: '10K' },
    { key: 'HALF_MARATHON', label: 'ADMIN.FILTER_HALF_MARATHON' },
    { key: 'MARATHON', label: 'Marathon' },
    { key: 'ULTRA', label: 'Ultra' },
  ];

  private readonly ultraTypes = new Set(['50K', '100K', 'Backyard Ultra', 'Catcher car']);

  availableYears = computed<number[]>(() => {
    const years = new Set<number>();
    for (const c of this.competitions()) {
      if (c.date) years.add(new Date(c.date).getFullYear());
    }
    return [...years].sort();
  });

  availableMonths = computed<number[]>(() => {
    const year = this.activeYearFilter();
    const months = new Set<number>();
    for (const c of this.competitions()) {
      if (!c.date) continue;
      const d = new Date(c.date);
      if (year !== null && d.getFullYear() !== year) continue;
      months.add(d.getMonth());
    }
    return [...months].sort((a, b) => a - b);
  });

  hasPastCompetitions = computed<boolean>(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.competitions().some(c => c.date && new Date(c.date) < today);
  });

  filteredCompetitions = computed<Competition[]>(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    let result = this.competitions();

    if (!this.showPastRaces()) {
      result = result.filter(c => !c.date || new Date(c.date) >= today);
    }

    const typeKey = this.activeTypeFilter();
    if (typeKey !== 'ALL') {
      result = result.filter(c => this.matchesTypeFilter(c, typeKey));
    }

    const year = this.activeYearFilter();
    if (year !== null) {
      result = result.filter(c => c.date && new Date(c.date).getFullYear() === year);
    }

    const month = this.activeMonthFilter();
    if (month !== null) {
      result = result.filter(c => c.date && new Date(c.date).getMonth() === month);
    }

    return result;
  });

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
      next: (data) => {
        const sorted = [...data].sort((a, b) => (a.date ?? '').localeCompare(b.date ?? ''));
        this.competitions.set(sorted);
      },
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

  private matchesTypeFilter(comp: Competition, filterKey: string): boolean {
    const formatTypes = (comp.formats ?? []).map(f => f.type);
    const allTypes = comp.type ? [comp.type, ...formatTypes] : formatTypes;

    switch (filterKey) {
      case '5K': return allTypes.includes('5K');
      case '10K': return allTypes.includes('10K');
      case 'HALF_MARATHON': return allTypes.includes('Halbmarathon');
      case 'MARATHON': return allTypes.includes('Marathon');
      case 'ULTRA': return allTypes.some(t => this.ultraTypes.has(t));
      default: return true;
    }
  }

  setTypeFilter(key: string): void {
    this.activeTypeFilter.set(key);
  }

  setYearFilter(year: number | null): void {
    this.activeYearFilter.set(year);
    this.activeMonthFilter.set(null);
  }

  setMonthFilter(month: number | null): void {
    this.activeMonthFilter.set(month);
  }

  togglePastRaces(): void {
    this.showPastRaces.update(v => !v);
  }

  formatMonth(month: number): string {
    const d = new Date(2024, month, 1);
    return d.toLocaleDateString('de-DE', { month: 'long' });
  }

  isPast(dateStr: string): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(dateStr) < today;
  }
}

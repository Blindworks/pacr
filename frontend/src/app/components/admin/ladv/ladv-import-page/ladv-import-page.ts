import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminLadvService } from '../../../../services/admin-ladv.service';
import {
  LadvImportRunSummary,
  LadvImportSource,
  LadvStagedEvent,
  LadvStagedEventStatus
} from '../../../../models/ladv.model';

@Component({
  selector: 'app-ladv-import-page',
  standalone: true,
  imports: [DatePipe, FormsModule, TranslateModule],
  templateUrl: './ladv-import-page.html',
  styleUrl: './ladv-import-page.scss'
})
export class LadvImportPage implements OnInit {
  private service = inject(AdminLadvService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  // Sources
  sources = signal<LadvImportSource[]>([]);
  sourcesLoading = signal(false);
  fetchingIds = signal<Set<number>>(new Set());
  lastFetchMessage = signal<string | null>(null);

  // Events
  events = signal<LadvStagedEvent[]>([]);
  eventsLoading = signal(false);
  totalElements = signal(0);
  page = signal(0);
  size = signal(50);

  // Filters (use signals; signal-bound to template inputs)
  filterSourceId = signal<number | null>(null);
  filterStatus = signal<LadvStagedEventStatus | ''>('NEW');
  filterQuery = signal('');

  busyEventIds = signal<Set<number>>(new Set());
  errorMessage = signal<string | null>(null);

  hasMorePages = computed(() => (this.page() + 1) * this.size() < this.totalElements());

  ngOnInit(): void {
    this.loadSources();
    this.loadEvents();
  }

  // ---------- Sources ----------

  loadSources(): void {
    this.sourcesLoading.set(true);
    this.service.listSources().subscribe({
      next: data => { this.sources.set(data); this.sourcesLoading.set(false); },
      error: () => this.sourcesLoading.set(false)
    });
  }

  goNewSource(): void {
    this.router.navigate(['/admin/ladv/sources/new']);
  }

  editSource(item: LadvImportSource): void {
    this.router.navigate(['/admin/ladv/sources', item.id, 'edit']);
  }

  deleteSource(item: LadvImportSource): void {
    const msg = this.translate.instant('ADMIN.LADV_SOURCE_CONFIRM_DELETE', { name: item.name });
    if (!confirm(msg)) return;
    this.service.deleteSource(item.id).subscribe({
      next: () => { this.loadSources(); this.loadEvents(); },
      error: err => alert(err?.error?.message ?? this.translate.instant('ADMIN.LADV_DELETE_ERROR'))
    });
  }

  fetchNow(item: LadvImportSource): void {
    const busy = new Set(this.fetchingIds());
    busy.add(item.id);
    this.fetchingIds.set(busy);
    this.lastFetchMessage.set(null);
    this.errorMessage.set(null);

    this.service.fetchNow(item.id).subscribe({
      next: (summary: LadvImportRunSummary) => {
        const done = new Set(this.fetchingIds());
        done.delete(item.id);
        this.fetchingIds.set(done);
        this.lastFetchMessage.set(
          this.translate.instant('ADMIN.LADV_FETCH_DONE', {
            name: item.name,
            newItems: summary.newItems,
            skipped: summary.skipped,
            fetched: summary.fetched
          })
        );
        this.loadSources();
        this.loadEvents();
      },
      error: err => {
        const done = new Set(this.fetchingIds());
        done.delete(item.id);
        this.fetchingIds.set(done);
        const msg = err?.status === 401
          ? this.translate.instant('ADMIN.LADV_NO_API_KEY')
          : (err?.error?.message
              ?? this.translate.instant('ADMIN.LADV_FETCH_ERROR', { name: item.name }));
        this.errorMessage.set(msg);
      }
    });
  }

  isFetching(id: number): boolean {
    return this.fetchingIds().has(id);
  }

  // ---------- Events ----------

  loadEvents(): void {
    this.eventsLoading.set(true);
    const status = this.filterStatus();
    this.service.listEvents({
      sourceId: this.filterSourceId() ?? undefined,
      status: status === '' ? undefined : status,
      q: this.filterQuery().trim() || undefined,
      page: this.page(),
      size: this.size()
    }).subscribe({
      next: p => {
        this.events.set(p.content);
        this.totalElements.set(p.totalElements);
        this.eventsLoading.set(false);
      },
      error: () => this.eventsLoading.set(false)
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadEvents();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadEvents();
    }
  }

  nextPage(): void {
    if (this.hasMorePages()) {
      this.page.set(this.page() + 1);
      this.loadEvents();
    }
  }

  adopt(event: LadvStagedEvent): void {
    const msg = this.translate.instant('ADMIN.LADV_ADOPT_CONFIRM', { name: event.name });
    if (!confirm(msg)) return;
    this.markBusy(event.id, true);
    this.service.adopt(event.id).subscribe({
      next: () => { this.markBusy(event.id, false); this.loadEvents(); },
      error: err => {
        this.markBusy(event.id, false);
        alert(err?.error?.message ?? this.translate.instant('ADMIN.LADV_ADOPT_ERROR'));
      }
    });
  }

  ignore(event: LadvStagedEvent): void {
    const msg = this.translate.instant('ADMIN.LADV_IGNORE_CONFIRM', { name: event.name });
    if (!confirm(msg)) return;
    this.markBusy(event.id, true);
    this.service.ignore(event.id).subscribe({
      next: () => { this.markBusy(event.id, false); this.loadEvents(); },
      error: err => {
        this.markBusy(event.id, false);
        alert(err?.error?.message ?? this.translate.instant('ADMIN.LADV_IGNORE_ERROR'));
      }
    });
  }

  reactivate(event: LadvStagedEvent): void {
    this.markBusy(event.id, true);
    this.service.reactivate(event.id).subscribe({
      next: () => { this.markBusy(event.id, false); this.loadEvents(); },
      error: err => {
        this.markBusy(event.id, false);
        alert(err?.error?.message ?? this.translate.instant('ADMIN.LADV_REACTIVATE_ERROR'));
      }
    });
  }

  openCompetition(event: LadvStagedEvent): void {
    if (event.importedCompetitionId) {
      this.router.navigate(['/admin/competitions', event.importedCompetitionId, 'edit']);
    }
  }

  isBusy(id: number): boolean {
    return this.busyEventIds().has(id);
  }

  formatDistances(event: LadvStagedEvent): string {
    if (!event.distances || event.distances.length === 0) return '—';
    return event.distances
      .map(d => d.name ?? (d.meters != null ? `${d.meters}m` : ''))
      .filter(s => !!s)
      .join(' · ');
  }

  // ---------- Helpers ----------

  private markBusy(id: number, busy: boolean): void {
    const set = new Set(this.busyEventIds());
    if (busy) set.add(id); else set.delete(id);
    this.busyEventIds.set(set);
  }

  onFilterSourceChange(value: string): void {
    this.filterSourceId.set(value === '' ? null : +value);
    this.applyFilters();
  }

  onFilterStatusChange(value: string): void {
    this.filterStatus.set(value as LadvStagedEventStatus | '');
    this.applyFilters();
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe, UpperCasePipe } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  AdminExternalNewsSourcesService,
  ExternalNewsSource,
  ImportRunSummary
} from '../../../../services/admin-external-news-sources.service';

@Component({
  selector: 'app-external-news-sources-list',
  standalone: true,
  imports: [DatePipe, UpperCasePipe, TranslateModule],
  templateUrl: './external-news-sources-list.html',
  styleUrl: './external-news-sources-list.scss'
})
export class ExternalNewsSourcesList implements OnInit {
  private service = inject(AdminExternalNewsSourcesService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  sources = signal<ExternalNewsSource[]>([]);
  isLoading = signal(false);
  fetchingIds = signal<Set<number>>(new Set());
  lastFetchMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.service.list().subscribe({
      next: data => { this.sources.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  goNew(): void {
    this.router.navigate(['/admin/news-sources/new']);
  }

  edit(item: ExternalNewsSource): void {
    this.router.navigate(['/admin/news-sources', item.id, 'edit']);
  }

  delete(item: ExternalNewsSource): void {
    const confirmMsg = this.translate.instant('ADMIN.NEWS_SOURCES_CONFIRM_DELETE', { name: item.name });
    if (!confirm(confirmMsg)) return;
    this.service.delete(item.id).subscribe({
      next: () => this.load(),
      error: err => {
        const msg = err?.error?.message ?? this.translate.instant('ADMIN.NEWS_SOURCES_DELETE_ERROR');
        alert(msg);
      }
    });
  }

  fetchNow(item: ExternalNewsSource): void {
    const busy = new Set(this.fetchingIds());
    busy.add(item.id);
    this.fetchingIds.set(busy);
    this.lastFetchMessage.set(null);

    this.service.fetchNow(item.id).subscribe({
      next: (summary: ImportRunSummary) => {
        const done = new Set(this.fetchingIds());
        done.delete(item.id);
        this.fetchingIds.set(done);
        this.lastFetchMessage.set(
          this.translate.instant('ADMIN.NEWS_SOURCES_FETCH_DONE', {
            name: item.name,
            newItems: summary.newItems
          })
        );
        this.load();
      },
      error: () => {
        const done = new Set(this.fetchingIds());
        done.delete(item.id);
        this.fetchingIds.set(done);
        this.lastFetchMessage.set(
          this.translate.instant('ADMIN.NEWS_SOURCES_FETCH_ERROR', { name: item.name })
        );
      }
    });
  }

  isFetching(id: number): boolean {
    return this.fetchingIds().has(id);
  }
}

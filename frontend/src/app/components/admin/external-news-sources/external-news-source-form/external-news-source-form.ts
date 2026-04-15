import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  AdminExternalNewsSourcesService,
  CreateExternalNewsSourceRequest
} from '../../../../services/admin-external-news-sources.service';

@Component({
  selector: 'app-external-news-source-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './external-news-source-form.html',
  styleUrl: './external-news-source-form.scss'
})
export class ExternalNewsSourceForm implements OnInit {
  private service = inject(AdminExternalNewsSourcesService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  sourceId = signal<number | null>(null);
  name = signal('');
  feedUrl = signal('');
  language = signal<'de' | 'en'>('de');
  enabled = signal(true);
  saving = signal(false);
  error = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.sourceId.set(+id);
      this.service.list().subscribe({
        next: list => {
          const item = list.find(s => s.id === +id);
          if (item) {
            this.name.set(item.name);
            this.feedUrl.set(item.feedUrl);
            this.language.set(item.language === 'en' ? 'en' : 'de');
            this.enabled.set(item.enabled);
          }
        }
      });
    }
  }

  save(): void {
    if (!this.name().trim() || !this.feedUrl().trim()) {
      this.error.set(this.translate.instant('ADMIN.NEWS_SOURCES_REQUIRED'));
      return;
    }
    const url = this.feedUrl().trim().toLowerCase();
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      this.error.set(this.translate.instant('ADMIN.NEWS_SOURCES_INVALID_URL'));
      return;
    }

    this.saving.set(true);
    this.error.set('');

    const data: CreateExternalNewsSourceRequest = {
      name: this.name().trim(),
      feedUrl: this.feedUrl().trim(),
      language: this.language(),
      enabled: this.enabled()
    };
    const id = this.sourceId();
    const call = id ? this.service.update(id, data) : this.service.create(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/news-sources']),
      error: err => {
        this.error.set(err?.error?.message
          ?? this.translate.instant('ADMIN.NEWS_SOURCES_SAVE_ERROR'));
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/news-sources']);
  }
}

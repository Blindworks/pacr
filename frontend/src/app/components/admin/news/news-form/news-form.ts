import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminNewsService, CreateNewsRequest } from '../../../../services/admin-news.service';

@Component({
  selector: 'app-news-form',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './news-form.html',
  styleUrl: './news-form.scss'
})
export class NewsForm implements OnInit {
  private newsService = inject(AdminNewsService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  newsId = signal<number | null>(null);
  title = signal('');
  content = signal('');
  excerpt = signal('');
  topicTag = signal('');
  heroImageFilename = signal('');
  isFeatured = signal(false);
  saving = signal(false);
  error = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.newsId.set(+id);
      this.newsService.getAll().subscribe({
        next: list => {
          const item = list.find(n => n.id === +id);
          if (item) {
            this.title.set(item.title);
            this.content.set(item.content);
            this.excerpt.set(item.excerpt ?? '');
            this.topicTag.set(item.topicTag ?? '');
            this.heroImageFilename.set(item.heroImageFilename ?? '');
            this.isFeatured.set(item.isFeatured ?? false);
          }
        }
      });
    }
  }

  save(): void {
    if (!this.title() || !this.content()) {
      this.error.set(this.translate.instant('ADMIN.NEWS_REQUIRED'));
      return;
    }

    this.saving.set(true);
    this.error.set('');

    const data: CreateNewsRequest = {
      title: this.title(),
      content: this.content(),
      excerpt: this.excerpt() || null,
      topicTag: this.topicTag() || null,
      heroImageFilename: this.heroImageFilename() || null,
      isFeatured: this.isFeatured()
    };
    const id = this.newsId();
    const call = id ? this.newsService.update(id, data) : this.newsService.create(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/news']),
      error: () => {
        this.error.set(this.translate.instant('ADMIN.NEWS_SAVE_ERROR'));
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/news']);
  }
}

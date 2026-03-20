import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminNewsService, CreateNewsRequest } from '../../../../services/admin-news.service';

@Component({
  selector: 'app-news-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './news-form.html',
  styleUrl: './news-form.scss'
})
export class NewsForm implements OnInit {
  private newsService = inject(AdminNewsService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  newsId = signal<number | null>(null);
  title = signal('');
  content = signal('');
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
          }
        }
      });
    }
  }

  save(): void {
    if (!this.title() || !this.content()) {
      this.error.set('Titel und Inhalt sind erforderlich.');
      return;
    }

    this.saving.set(true);
    this.error.set('');

    const data: CreateNewsRequest = { title: this.title(), content: this.content() };
    const id = this.newsId();
    const call = id ? this.newsService.update(id, data) : this.newsService.create(data);

    call.subscribe({
      next: () => this.router.navigate(['/admin/news']),
      error: () => {
        this.error.set('Fehler beim Speichern. Bitte erneut versuchen.');
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/news']);
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AdminNewsService, AppNews } from '../../../../services/admin-news.service';

@Component({
  selector: 'app-news-list',
  standalone: true,
  imports: [DatePipe, TranslateModule],
  templateUrl: './news-list.html',
  styleUrl: './news-list.scss'
})
export class NewsList implements OnInit {
  private newsService = inject(AdminNewsService);
  private router = inject(Router);

  news = signal<AppNews[]>([]);
  isLoading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.newsService.getAll().subscribe({
      next: data => { this.news.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  goNew(): void {
    this.router.navigate(['/admin/news/new']);
  }

  edit(item: AppNews): void {
    this.router.navigate(['/admin/news', item.id, 'edit']);
  }

  publish(item: AppNews): void {
    this.newsService.publish(item.id).subscribe({
      next: () => this.load()
    });
  }

  delete(item: AppNews): void {
    if (!confirm(`News "${item.title}" wirklich löschen?`)) return;
    this.newsService.delete(item.id).subscribe({
      next: () => this.load()
    });
  }
}

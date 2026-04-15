import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

const BASE = apiUrl('/news');

export interface PublicNews {
  id: number;
  title: string;
  content: string;
  excerpt: string | null;
  topicTag: string | null;
  heroImageFilename: string | null;
  isFeatured: boolean;
  isPublished: boolean;
  publishedAt: string | null;
  createdAt: string;
}

export interface TrendingTopic {
  tag: string;
  viewCount: number;
  newsCount: number;
  headline: string | null;
}

@Injectable({ providedIn: 'root' })
export class PublicNewsService {
  private readonly http = inject(HttpClient);

  listPublished(): Observable<PublicNews[]> {
    return this.http.get<PublicNews[]>(BASE);
  }

  getFeatured(): Observable<PublicNews | null> {
    return this.http.get<PublicNews | null>(`${BASE}/featured`);
  }

  get(id: number): Observable<PublicNews> {
    return this.http.get<PublicNews>(`${BASE}/${id}`);
  }

  recordView(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/view`, {});
  }

  getTrending(): Observable<TrendingTopic[]> {
    return this.http.get<TrendingTopic[]>(`${BASE}/trending`);
  }
}

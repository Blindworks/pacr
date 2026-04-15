import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AppNews {
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

export interface CreateNewsRequest {
  title: string;
  content: string;
  excerpt?: string | null;
  topicTag?: string | null;
  heroImageFilename?: string | null;
  isFeatured?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminNewsService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/news');

  getAll(): Observable<AppNews[]> {
    return this.http.get<AppNews[]>(this.base);
  }

  create(data: CreateNewsRequest): Observable<AppNews> {
    return this.http.post<AppNews>(this.base, data);
  }

  update(id: number, data: CreateNewsRequest): Observable<AppNews> {
    return this.http.put<AppNews>(`${this.base}/${id}`, data);
  }

  publish(id: number): Observable<AppNews> {
    return this.http.post<AppNews>(`${this.base}/${id}/publish`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

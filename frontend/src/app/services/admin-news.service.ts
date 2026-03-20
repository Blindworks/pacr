import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AppNews {
  id: number;
  title: string;
  content: string;
  isPublished: boolean;
  publishedAt: string | null;
  createdAt: string;
}

export interface CreateNewsRequest {
  title: string;
  content: string;
}

@Injectable({ providedIn: 'root' })
export class AdminNewsService {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api/admin/news';

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

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

export interface ExternalNewsSource {
  id: number;
  name: string;
  feedUrl: string;
  language: string;
  enabled: boolean;
  lastFetchedAt: string | null;
  lastFetchStatus: string | null;
  createdAt: string;
}

export interface CreateExternalNewsSourceRequest {
  name: string;
  feedUrl: string;
  language: string;
  enabled?: boolean;
}

export interface ImportRunSummary {
  sourcesProcessed: number;
  newItems: number;
  results: {
    sourceId: number;
    sourceName: string;
    newItems: number;
    skippedDuplicates: number;
    success: boolean;
    message: string;
  }[];
}

@Injectable({ providedIn: 'root' })
export class AdminExternalNewsSourcesService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/news-sources');

  list(): Observable<ExternalNewsSource[]> {
    return this.http.get<ExternalNewsSource[]>(this.base);
  }

  create(data: CreateExternalNewsSourceRequest): Observable<ExternalNewsSource> {
    return this.http.post<ExternalNewsSource>(this.base, data);
  }

  update(id: number, data: CreateExternalNewsSourceRequest): Observable<ExternalNewsSource> {
    return this.http.put<ExternalNewsSource>(`${this.base}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  fetchNow(id: number): Observable<ImportRunSummary> {
    return this.http.post<ImportRunSummary>(`${this.base}/${id}/fetch`, {});
  }
}

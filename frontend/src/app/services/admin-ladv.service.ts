import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';
import {
  LadvCreateSourceRequest,
  LadvImportRunSummary,
  LadvImportSource,
  LadvLvOption,
  LadvStagedEventsPage,
  LadvStagedEventStatus
} from '../models/ladv.model';

@Injectable({ providedIn: 'root' })
export class AdminLadvService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/ladv');

  listSources(): Observable<LadvImportSource[]> {
    return this.http.get<LadvImportSource[]>(`${this.base}/sources`);
  }

  getSource(id: number): Observable<LadvImportSource> {
    return this.http.get<LadvImportSource>(`${this.base}/sources/${id}`);
  }

  createSource(data: LadvCreateSourceRequest): Observable<LadvImportSource> {
    return this.http.post<LadvImportSource>(`${this.base}/sources`, data);
  }

  updateSource(id: number, data: LadvCreateSourceRequest): Observable<LadvImportSource> {
    return this.http.put<LadvImportSource>(`${this.base}/sources/${id}`, data);
  }

  deleteSource(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/sources/${id}`);
  }

  fetchNow(id: number): Observable<LadvImportRunSummary> {
    return this.http.post<LadvImportRunSummary>(`${this.base}/sources/${id}/fetch`, {});
  }

  listEvents(opts: {
    sourceId?: number;
    status?: LadvStagedEventStatus;
    q?: string;
    page?: number;
    size?: number;
  } = {}): Observable<LadvStagedEventsPage> {
    let params = new HttpParams();
    if (opts.sourceId != null) params = params.set('sourceId', String(opts.sourceId));
    if (opts.status) params = params.set('status', opts.status);
    if (opts.q) params = params.set('q', opts.q);
    if (opts.page != null) params = params.set('page', String(opts.page));
    if (opts.size != null) params = params.set('size', String(opts.size));
    return this.http.get<LadvStagedEventsPage>(`${this.base}/events`, { params });
  }

  adopt(eventId: number): Observable<{ competitionId: number; name: string }> {
    return this.http.post<{ competitionId: number; name: string }>(
      `${this.base}/events/${eventId}/adopt`, {});
  }

  ignore(eventId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/events/${eventId}/ignore`, {});
  }

  reactivate(eventId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/events/${eventId}/reactivate`, {});
  }

  listLvs(): Observable<LadvLvOption[]> {
    return this.http.get<LadvLvOption[]>(`${this.base}/lvs`);
  }
}

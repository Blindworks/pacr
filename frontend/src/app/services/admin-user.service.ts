import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';
import { UserSummary } from './admin-login-message.service';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/users');

  searchUsers(query: string, limit = 20): Observable<UserSummary[]> {
    const params = new HttpParams().set('q', query).set('limit', String(limit));
    return this.http.get<UserSummary[]>(`${this.base}/search`, { params });
  }

  getUserSummary(id: number): Observable<UserSummary> {
    return this.http.get<UserSummary>(`${this.base}/${id}/summary`);
  }
}

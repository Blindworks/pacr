import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CorosStatus {
  connected: boolean;
  nickname: string | null;
  profilePhoto: string | null;
  openId: string | null;
}

@Injectable({ providedIn: 'root' })
export class CorosService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/coros');

  getStatus(): Observable<CorosStatus> {
    return this.http.get<CorosStatus>(`${this.base}/connection-status`);
  }

  getAuthUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/auth-url`);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${this.base}/disconnect`);
  }
}

import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StravaStatus {
  connected: boolean;
  athleteName: string | null;
  athleteCity: string | null;
  profileMedium: string | null;
}

@Injectable({ providedIn: 'root' })
export class StravaService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/strava');

  getStatus(): Observable<StravaStatus> {
    return this.http.get<StravaStatus>(`${this.base}/status`);
  }

  getAuthUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/auth-url`);
  }

  syncActivities(startDate: string, endDate: string): Observable<unknown> {
    return this.http.get(`${this.base}/activities?startDate=${startDate}&endDate=${endDate}`);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${this.base}/disconnect`);
  }
}

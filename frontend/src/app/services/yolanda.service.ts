import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface YolandaStatus {
  connected: boolean;
  nickname: string | null;
  accountId: string | null;
}

export interface BodyMeasurement {
  id: number;
  measuredAt: string;
  weightKg: number | null;
  fatPercentage: number | null;
  waterPercentage: number | null;
  muscleMassKg: number | null;
  boneMassKg: number | null;
  visceralFatLevel: number | null;
  metabolicAge: number | null;
  bmi: number | null;
  source: string;
}

@Injectable({ providedIn: 'root' })
export class YolandaService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/yolanda');

  getStatus(): Observable<YolandaStatus> {
    return this.http.get<YolandaStatus>(`${this.base}/status`);
  }

  getAuthUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/auth-url`);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${this.base}/disconnect`);
  }

  sync(from: string, to: string): Observable<BodyMeasurement[]> {
    return this.http.post<BodyMeasurement[]>(`${this.base}/sync?from=${from}&to=${to}`, {});
  }
}

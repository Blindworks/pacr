import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = apiUrl('/completed-trainings');
const BODY_METRICS_BASE = apiUrl('/body-metrics');

export interface Vo2MaxPoint {
  date: string;
  vo2max: number;
}

export interface StatsBucket {
  label: string;
  startDate: string;
  endDate: string;
  distanceKm: number;
  durationSeconds: number;
  elevationGainM: number;
  activityCount: number;
}

export interface TrainingStatsDto {
  totalDistanceKm: number;
  totalDurationSeconds: number;
  totalActivityCount: number;
  avgPaceSecondsPerKm: number;
  avgHeartRate?: number;
  totalElevationGainM?: number;
  totalZone1Seconds: number;
  totalZone2Seconds: number;
  totalZone3Seconds: number;
  totalZone4Seconds: number;
  totalZone5Seconds: number;
  buckets: StatsBucket[];
}

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly http = inject(HttpClient);

  getStats(period: string = 'month'): Observable<TrainingStatsDto> {
    const params = new HttpParams().set('period', period);
    return this.http.get<TrainingStatsDto>(`${BASE}/stats`, { params });
  }

  getStatsForDateRange(from: string, to: string): Observable<TrainingStatsDto> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<TrainingStatsDto>(`${BASE}/stats`, { params });
  }

  getVo2MaxHistory(): Observable<Vo2MaxPoint[]> {
    return this.http.get<Vo2MaxPoint[]>(`${BODY_METRICS_BASE}/vo2max-history`);
  }

  recalculateBodyMetrics(): Observable<{ message: string; activitiesProcessed: number }> {
    return this.http.post<{ message: string; activitiesProcessed: number }>(
      `${BODY_METRICS_BASE}/recalculate`, {});
  }
}

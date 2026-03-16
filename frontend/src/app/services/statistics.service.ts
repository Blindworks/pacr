import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/completed-trainings';

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
}

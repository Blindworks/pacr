import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/completed-trainings';

export interface CompletedTraining {
  id: number;
  trainingDate: string;
  uploadDate: string;
  activityName: string | null;
  trainingType: string | null;
  sport: string | null;
  subSport: string | null;
  source: string;
  distanceKm: number | null;
  durationSeconds: number | null;
  movingTimeSeconds: number | null;
  averagePaceSecondsPerKm: number | null;
  averageSpeedKmh: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  elevationGainM: number | null;
  calories: number | null;
  averageCadence: number | null;
  averagePowerWatts: number | null;
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);

  getActivities(page = 0, size = 20): Observable<CompletedTraining[]> {
    return this.http.get<CompletedTraining[]>(`${BASE}?page=${page}&size=${size}`);
  }

  getByDateRange(startDate: string, endDate: string): Observable<CompletedTraining[]> {
    return this.http.get<CompletedTraining[]>(`${BASE}/by-date-range?startDate=${startDate}&endDate=${endDate}`);
  }

  getById(id: number): Observable<CompletedTraining> {
    return this.http.get<CompletedTraining>(`${BASE}/${id}`);
  }
}

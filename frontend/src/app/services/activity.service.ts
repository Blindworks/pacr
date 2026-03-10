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

export interface ActivityStreamDto {
  completedTrainingId: number;
  distancePoints: number[];
  heartRate: (number | null)[];
  altitude: (number | null)[];
  paceSecondsPerKm: (number | null)[];
  hasHeartRate: boolean;
  hasAltitude: boolean;
  hasPace: boolean;
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

  getStreams(id: number): Observable<ActivityStreamDto> {
    return this.http.get<ActivityStreamDto>(`${BASE}/${id}/streams`);
  }

  fetchStreams(id: number): Observable<ActivityStreamDto> {
    return this.http.post<ActivityStreamDto>(`${BASE}/${id}/fetch-streams`, null);
  }
}

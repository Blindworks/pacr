import { apiUrl } from '../core/api-base';
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TrainingStep {
  id: number;
  sortOrder: number;
  stepType: string;
  title?: string;
  subtitle?: string;
  durationMinutes?: number;
  durationSeconds?: number;
  distanceMeters?: number;
  paceDisplay?: string;
  icon?: string;
  highlight?: boolean;
  muted?: boolean;
}

export interface TrainingPrepTip {
  id: number;
  sortOrder: number;
  icon?: string;
  title: string;
  text?: string;
}

export interface Training {
  id: number;
  name: string;
  description?: string;
  weekNumber?: number;
  dayOfWeek?: number | string;
  intensityLevel?: string;
  trainingType?: string;
  durationMinutes?: number;
  workPace?: string;
  recoveryPace?: string;
  intensityScore?: number;
  estimatedCalories?: number;
  benefit?: string;
  estimatedDistanceMeters?: number;
  difficulty?: string;
  heroImageUrl?: string;
  steps: TrainingStep[];
  prepTips: TrainingPrepTip[];
}

@Injectable({ providedIn: 'root' })
export class TrainingService {
  private readonly baseUrl = apiUrl();

  constructor(private http: HttpClient) {}

  getAll(): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings`);
  }

  getByPlan(planId: number): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings/plan/${planId}`);
  }

  getTrainingById(id: number): Observable<Training> {
    return this.http.get<Training>(`${this.baseUrl}/trainings/${id}`);
  }

  create(payload: Partial<Training>, planId?: number): Observable<Training> {
    let params = new HttpParams();
    if (planId != null) params = params.set('planId', planId);
    return this.http.post<Training>(`${this.baseUrl}/trainings`, payload, { params });
  }

  update(id: number, payload: Partial<Training>): Observable<Training> {
    return this.http.put<Training>(`${this.baseUrl}/trainings/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/trainings/${id}`);
  }
}

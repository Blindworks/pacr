import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TrainingStep {
  id: number;
  sortOrder: number;
  stepType: string;
  title: string;
  subtitle?: string;
  durationMinutes?: number;
  paceDisplay?: string;
  icon?: string;
  highlight?: boolean;
  muted?: boolean;
  repetitions?: number;
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
  trainingDate?: string;
  intensityLevel?: string;
  trainingType?: string;
  duration?: number;
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
  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getTrainingById(id: number): Observable<Training> {
    return this.http.get<Training>(`${this.baseUrl}/trainings/${id}`);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TrainingPlan {
  id: number;
  name: string;
  description?: string;
  targetTime?: string;
  prerequisites?: string;
  competitionType?: string;
  trainingCount?: number;
  uploadDate?: string;
}

@Injectable({ providedIn: 'root' })
export class TrainingPlanService {
  private readonly baseUrl = 'http://localhost:8080/api/training-plans';

  constructor(private http: HttpClient) {}

  getAll(): Observable<TrainingPlan[]> {
    return this.http.get<TrainingPlan[]>(this.baseUrl);
  }

  getById(id: number): Observable<TrainingPlan> {
    return this.http.get<TrainingPlan>(`${this.baseUrl}/${id}`);
  }

  create(payload: Partial<TrainingPlan>): Observable<TrainingPlan> {
    return this.http.post<TrainingPlan>(this.baseUrl, payload);
  }

  update(id: number, payload: Partial<TrainingPlan>): Observable<TrainingPlan> {
    return this.http.put<TrainingPlan>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getTemplates(): Observable<TrainingPlan[]> {
    return this.http.get<TrainingPlan[]>(`${this.baseUrl}/templates`);
  }

  assignToCompetition(planId: number, competitionId: number): Observable<TrainingPlan> {
    return this.http.post<TrainingPlan>(`${this.baseUrl}/assign`, null, {
      params: { planId: planId.toString(), competitionId: competitionId.toString() }
    });
  }
}

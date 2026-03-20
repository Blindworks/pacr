import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserTrainingEntry {
  id: number;
  training: {
    id: number;
    name: string;
    description?: string;
    trainingType?: string;
    intensityLevel?: string;
    durationMinutes?: number;
    estimatedDistanceMeters?: number;
    trainingPlanName?: string;
    trainingPlanId?: number;
  };
  trainingDate: string;
  weekNumber: number;
  completed: boolean;
  completionStatus?: string;
  registrationId: number;
  competitionId: number;
  competitionName?: string;
  originalTrainingDate?: string;
}

export interface TrainingFeedbackPayload {
  completed: boolean;
  completionStatus: string;
}

@Injectable({ providedIn: 'root' })
export class UserTrainingEntryService {
  private http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api/user-training-entries';

  getCalendar(from: string, to: string): Observable<UserTrainingEntry[]> {
    return this.http.get<UserTrainingEntry[]>(`${this.base}/calendar`, {
      params: { from, to }
    });
  }

  updateFeedback(id: number, payload: TrainingFeedbackPayload): Observable<UserTrainingEntry> {
    return this.http.put<UserTrainingEntry>(`${this.base}/${id}/feedback`, payload);
  }
}

import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AsthmaRiskDto {
  riskIndex: number;
  pollenSummary: string;
  level: string;
}

export interface CyclePhaseDto {
  phase: string;
  energyLevel: number;
  recommendation: string;
}

export interface ExistingSessionDto {
  id: number;
  aiRecommendation: string;
  aiSuggestedAction: string;
  userDecision: string;
}

export interface UserTrainingEntrySimple {
  id: number;
  trainingDate: string;
  weekNumber: number;
  completed: boolean;
  completionStatus: string;
  training: {
    id: number;
    name: string;
    type: string;
    intensity: string;
    duration: number;
    description: string;
  };
  competitionName: string;
}

export interface DailyCoachContextDto {
  date: string;
  plannedEntries: UserTrainingEntrySimple[];
  asthmaRisk?: AsthmaRiskDto;
  cyclePhase?: CyclePhaseDto;
  existingSession?: ExistingSessionDto;
}

export interface RecommendationRequest {
  date: string;
  feelingScore: number;
  feelingText: string;
}

export interface RecommendationResponse {
  sessionId: number;
  recommendationText: string;
  suggestedAction: string;
  restructuringPreview: string;
}

export interface ExecuteRequest {
  sessionId: number;
  decision: string;
}

export interface RestructuringChange {
  entryId: number;
  action: string;
  newDate?: string;
}

export interface ExecuteResponse {
  message: string;
  changes: RestructuringChange[];
}

@Injectable({ providedIn: 'root' })
export class DailyCoachService {
  private http = inject(HttpClient);
  private baseUrl = apiUrl('/ai-trainer');

  getContext(date: string): Observable<DailyCoachContextDto> {
    return this.http.get<DailyCoachContextDto>(`${this.baseUrl}/context?date=${date}`);
  }

  getRecommendation(req: RecommendationRequest): Observable<RecommendationResponse> {
    return this.http.post<RecommendationResponse>(`${this.baseUrl}/recommendation`, req);
  }

  executeDecision(req: ExecuteRequest): Observable<ExecuteResponse> {
    return this.http.post<ExecuteResponse>(`${this.baseUrl}/execute`, req);
  }
}

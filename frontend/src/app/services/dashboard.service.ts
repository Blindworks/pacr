import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/dashboard';

export interface LoadTrendPoint {
  date: string;
  strain21: number;
}

export interface EfTrendPoint {
  date: string;
  ef: number;
}

export interface DriftTrendPoint {
  date: string;
  driftPct: number;
}

export interface LastRun {
  date: string;
  strain21: number;
  driftPct: number;
  z4Min: number;
  z5Min: number;
  coachBullets: string[];
}

export interface NextCompetition {
  competitionName: string;
  competitionLocation: string;
  date: string;
  daysUntil: number;
  elapsedPct: number;
}

export interface TrainingProgress {
  competitionId: number;
  competitionName: string;
  competitionDate: string;
  total: number;
  completed: number;
  completionPct: number;
}

export interface DashboardData {
  readinessScore: number;
  readinessRecommendation: string;
  readinessReasons: string[];
  strain21: number;
  loadStatus: {
    acwr: number;
    flag: string;
  };
  loadTrend: LoadTrendPoint[];
  efTrend: EfTrendPoint[];
  driftTrend: DriftTrendPoint[];
  lastRun: LastRun | null;
  nextCompetition: NextCompetition | null;
  trainingProgress: TrainingProgress[];
  bodyBattery?: number;
  vo2max?: number;
  vo2maxDate?: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getDashboard(): Observable<DashboardData> {
    return this.http.get<DashboardData>(BASE);
  }
}

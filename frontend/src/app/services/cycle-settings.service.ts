import { apiUrl } from '../core/api-base';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CycleSettings {
  id?: number;
  firstDayOfLastPeriod: string; // YYYY-MM-DD
  averageCycleLength: number;
  averagePeriodDuration: number;
}

export interface CycleStatusDto {
  currentPhase: string;
  currentDay: number;
  cycleLength: number;
  daysRemainingInPhase: number;
  nextPhase: string;
  shouldShowNewCyclePrompt: boolean;
  periodDuration: number;
}

@Injectable({ providedIn: 'root' })
export class CycleSettingsService {
  private readonly base = apiUrl('/cycle-settings');

  constructor(private http: HttpClient) {}

  getSettings(): Observable<CycleSettings> {
    return this.http.get<CycleSettings>(this.base);
  }

  saveSettings(settings: CycleSettings): Observable<CycleSettings> {
    return this.http.post<CycleSettings>(this.base, settings);
  }

  getStatus(): Observable<CycleStatusDto> {
    return this.http.get<CycleStatusDto>(`${this.base}/status`);
  }
}

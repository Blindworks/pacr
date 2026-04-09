import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BotProfileDto {
  id: number;
  userId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;

  cityName?: string;
  homeLatitude: number;
  homeLongitude: number;
  searchRadiusKm: number;

  gender?: string;
  age?: number;

  paceMinSecPerKm: number;
  paceMaxSecPerKm: number;
  distanceMinKm: number;
  distanceMaxKm: number;

  maxHeartRate?: number;
  restingHeartRate?: number;

  scheduleDays: string[];
  scheduleStartTime?: string; // "HH:mm:ss"
  scheduleJitterMinutes: number;

  nextScheduledRunAt?: string;
  lastRunAt?: string;
  lastRunStatus?: string;
  lastRunMessage?: string;

  includeInLeaderboard: boolean;
  enabled: boolean;

  createdAt?: string;
  updatedAt?: string;
}

export interface BotCreateRequest {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;

  cityName?: string;
  homeLatitude: number;
  homeLongitude: number;
  searchRadiusKm?: number;

  gender?: string;
  age?: number;

  paceMinSecPerKm: number;
  paceMaxSecPerKm: number;
  distanceMinKm: number;
  distanceMaxKm: number;

  maxHeartRate?: number;
  restingHeartRate?: number;

  scheduleDays?: string[];
  scheduleStartTime?: string;
  scheduleJitterMinutes?: number;

  includeInLeaderboard?: boolean;
  enabled?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminBotRunnerService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/bot-runners');

  getAll(): Observable<BotProfileDto[]> {
    return this.http.get<BotProfileDto[]>(this.base);
  }

  get(id: number): Observable<BotProfileDto> {
    return this.http.get<BotProfileDto>(`${this.base}/${id}`);
  }

  create(data: BotCreateRequest): Observable<BotProfileDto> {
    return this.http.post<BotProfileDto>(this.base, data);
  }

  update(id: number, data: BotCreateRequest): Observable<BotProfileDto> {
    return this.http.put<BotProfileDto>(`${this.base}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  runNow(id: number): Observable<BotProfileDto> {
    return this.http.post<BotProfileDto>(`${this.base}/${id}/run-now`, {});
  }
}

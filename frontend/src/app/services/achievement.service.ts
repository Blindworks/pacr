import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = apiUrl('/achievements');

export interface Achievement {
  id: number;
  key: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  threshold: number;
  currentValue: number | null;
  unlocked: boolean;
  unlockedAt: string | null;
  progress: number;
}

export interface StreakInfo {
  currentStreak: number;
  longestStreak: number;
}

@Injectable({ providedIn: 'root' })
export class AchievementService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Achievement[]> {
    return this.http.get<Achievement[]>(BASE);
  }

  getRecent(): Observable<Achievement[]> {
    return this.http.get<Achievement[]>(`${BASE}/recent`);
  }

  getStreak(): Observable<StreakInfo> {
    return this.http.get<StreakInfo>(`${BASE}/streak`);
  }
}

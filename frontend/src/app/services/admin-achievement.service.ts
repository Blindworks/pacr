import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdminAchievement {
  id: number;
  key: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  threshold: number;
  sortOrder: number;
  validFrom: string | null;
  validUntil: string | null;
  timeBound: boolean;
  active: boolean;
  expired: boolean;
  unlockedCount: number;
  inProgressCount: number;
  unlockedUsers?: UnlockedUser[];
}

export interface UnlockedUser {
  userId: number;
  username: string;
  firstName: string | null;
  lastName: string | null;
  currentValue: number;
  unlockedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminAchievementService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/achievements');

  getAll(): Observable<AdminAchievement[]> {
    return this.http.get<AdminAchievement[]>(this.base);
  }

  getById(id: number): Observable<AdminAchievement> {
    return this.http.get<AdminAchievement>(`${this.base}/${id}`);
  }

  create(data: Partial<AdminAchievement>): Observable<AdminAchievement> {
    return this.http.post<AdminAchievement>(this.base, data);
  }

  update(id: number, data: Partial<AdminAchievement>): Observable<AdminAchievement> {
    return this.http.put<AdminAchievement>(`${this.base}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

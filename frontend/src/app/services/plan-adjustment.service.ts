import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PlanAdjustment {
  id: number;
  adjustmentType: 'RESCHEDULE' | 'DROP' | 'INTENSITY_REDUCE';
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  reason: string;
  triggerSource: string;
  originalDate: string;
  newDate?: string;
  originalIntensity?: string;
  newIntensity?: string;
  trainingName?: string;
  userTrainingEntryId?: number;
  createdAt: string;
  resolvedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class PlanAdjustmentService {
  private http = inject(HttpClient);
  private baseUrl = apiUrl('/plan-adjustments');

  getPending(): Observable<PlanAdjustment[]> {
    return this.http.get<PlanAdjustment[]>(`${this.baseUrl}/pending`);
  }

  accept(id: number): Observable<PlanAdjustment> {
    return this.http.post<PlanAdjustment>(`${this.baseUrl}/${id}/accept`, {});
  }

  reject(id: number): Observable<PlanAdjustment> {
    return this.http.post<PlanAdjustment>(`${this.baseUrl}/${id}/reject`, {});
  }

  getHistory(): Observable<PlanAdjustment[]> {
    return this.http.get<PlanAdjustment[]>(`${this.baseUrl}/history`);
  }
}

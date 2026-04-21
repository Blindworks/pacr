import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdminRegistration {
  id: number;
  userId: number;
  userEmail: string;
  userDisplayName: string;
  competitionId: number;
  competitionName: string;
  competitionDate: string;
  trainingPlanId?: number;
  trainingPlanName?: string;
  competitionFormatId?: number;
  competitionFormatType?: string;
  registeredAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminRegistrationService {
  private http = inject(HttpClient);
  private base = apiUrl('/admin/registrations');

  getAll(): Observable<AdminRegistration[]> {
    return this.http.get<AdminRegistration[]>(this.base);
  }

  deleteRegistration(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  deleteCompetition(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/admin/competitions/${id}`));
  }
}

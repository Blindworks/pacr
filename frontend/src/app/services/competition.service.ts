import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Competition {
  id: number;
  name: string;
  date: string;
  description?: string;
  type?: string;
  location?: string;
  registered?: boolean;
  registeredWithOrganizer?: boolean;
  trainingPlanId?: number;
  trainingPlanName?: string;
}

@Injectable({ providedIn: 'root' })
export class CompetitionService {
  private http = inject(HttpClient);
  private base = apiUrl('/competitions');

  getAll(): Observable<Competition[]> {
    return this.http.get<Competition[]>(this.base);
  }

  getById(id: number): Observable<Competition> {
    return this.http.get<Competition>(`${this.base}/${id}`);
  }

  create(data: Partial<Competition>): Observable<Competition> {
    return this.http.post<Competition>(this.base, data);
  }

  update(id: number, data: Partial<Competition>): Observable<Competition> {
    return this.http.put<Competition>(`${this.base}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  updateRegistration(id: number, registeredWithOrganizer: boolean): Observable<unknown> {
    return this.http.put(`${this.base}/${id}/register`, { registeredWithOrganizer });
  }
}

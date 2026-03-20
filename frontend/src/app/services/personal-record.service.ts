import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/personal-records';

export interface PersonalRecord {
  id: number;
  distanceKm: number;
  distanceLabel: string;
  bestTimeSeconds?: number;
  goalTimeSeconds?: number;
  achievedDate?: string;
  isManual?: boolean;
  activityId?: number;
}

export interface PersonalRecordEntry {
  id: number;
  timeSeconds: number;
  achievedDate: string;
  isManual: boolean;
  activityId?: number;
}

export interface CreatePersonalRecordRequest {
  distanceKm: number;
  distanceLabel: string;
  goalTimeSeconds?: number;
}

export interface AddEntryRequest {
  timeSeconds: number;
  achievedDate: string;
}

@Injectable({ providedIn: 'root' })
export class PersonalRecordService {
  private readonly http = inject(HttpClient);

  getPersonalRecords(): Observable<PersonalRecord[]> {
    return this.http.get<PersonalRecord[]>(BASE);
  }

  createPersonalRecord(req: CreatePersonalRecordRequest): Observable<PersonalRecord> {
    return this.http.post<PersonalRecord>(BASE, req);
  }

  updatePersonalRecordGoal(id: number, goalTimeSeconds: number | null): Observable<PersonalRecord> {
    return this.http.put<PersonalRecord>(`${BASE}/${id}`, { goalTimeSeconds });
  }

  deletePersonalRecord(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  getPersonalRecordEntries(id: number): Observable<PersonalRecordEntry[]> {
    return this.http.get<PersonalRecordEntry[]>(`${BASE}/${id}/entries`);
  }

  addPersonalRecordEntry(id: number, req: AddEntryRequest): Observable<PersonalRecordEntry> {
    return this.http.post<PersonalRecordEntry>(`${BASE}/${id}/entries`, req);
  }

  deletePersonalRecordEntry(id: number, entryId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}/entries/${entryId}`);
  }
}

import { apiUrl } from '../core/api-base';
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
export interface CycleEntry {
  id?: number;
  entryDate: string;           // YYYY-MM-DD
  physicalSymptoms?: string;   // comma-separated: "CRAMPS,HEADACHE"
  mood?: string;
  energyLevel?: number;
  sleepHours?: number;
  sleepMinutes?: number;
  sleepQuality?: string;
  flowIntensity?: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class CycleEntryService {
  private readonly base = apiUrl('/cycle-entries');

  constructor(private http: HttpClient) {}

  getAll(): Observable<CycleEntry[]> {
    return this.http.get<CycleEntry[]>(this.base);
  }

  getLatest(): Observable<CycleEntry> {
    return this.http.get<CycleEntry>(`${this.base}/latest`);
  }

  getByDate(date: Date): Observable<CycleEntry> {
    const params = new HttpParams().set('date', date.toISOString().split('T')[0]);
    return this.http.get<CycleEntry>(`${this.base}/by-date`, { params });
  }

  create(entry: CycleEntry): Observable<CycleEntry> {
    return this.http.post<CycleEntry>(this.base, entry);
  }

  update(id: number, entry: CycleEntry): Observable<CycleEntry> {
    return this.http.put<CycleEntry>(`${this.base}/${id}`, entry);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

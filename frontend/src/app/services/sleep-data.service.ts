import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/sleep-data';

export interface SleepDataEntry {
  id?: number;
  recordedAt: string;
  restingHeartRate?: number;
  sleepScore?: number;
  bodyBattery?: number;
  spO2?: number;
}

@Injectable({ providedIn: 'root' })
export class SleepDataService {
  private readonly http = inject(HttpClient);

  getLatest(): Observable<SleepDataEntry> {
    return this.http.get<SleepDataEntry>(`${BASE}/latest`);
  }

  create(entry: SleepDataEntry): Observable<SleepDataEntry> {
    return this.http.post<SleepDataEntry>(BASE, entry);
  }
}

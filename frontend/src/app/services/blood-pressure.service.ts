import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = apiUrl('/blood-pressure');

export interface BloodPressureEntry {
  id?: number;
  measuredAt: string;
  systolicPressure: number;
  diastolicPressure: number;
  pulseAtMeasurement?: number;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class BloodPressureService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<BloodPressureEntry[]> {
    return this.http.get<BloodPressureEntry[]>(BASE);
  }

  getLatest(): Observable<BloodPressureEntry> {
    return this.http.get<BloodPressureEntry>(`${BASE}/latest`);
  }

  create(entry: BloodPressureEntry): Observable<BloodPressureEntry> {
    return this.http.post<BloodPressureEntry>(BASE, entry);
  }
}

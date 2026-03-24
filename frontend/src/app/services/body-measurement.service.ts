import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = apiUrl('/body-measurements');

export interface BodyMeasurementEntry {
  id?: number;
  measuredAt: string;
  weightKg?: number;
  fatPercentage?: number;
  waterPercentage?: number;
  muscleMassKg?: number;
  boneMassKg?: number;
  visceralFatLevel?: number;
  metabolicAge?: number;
  bmi?: number;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class BodyMeasurementService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<BodyMeasurementEntry[]> {
    return this.http.get<BodyMeasurementEntry[]>(BASE);
  }

  getLatest(): Observable<BodyMeasurementEntry> {
    return this.http.get<BodyMeasurementEntry>(`${BASE}/latest`);
  }

  create(entry: BodyMeasurementEntry): Observable<BodyMeasurementEntry> {
    return this.http.post<BodyMeasurementEntry>(BASE, entry);
  }
}

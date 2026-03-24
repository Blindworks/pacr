import { apiUrl } from '../core/api-base';
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BioWeatherDto {
  regionId: number;
  regionName: string | null;
  pollenBirch: number | null;
  pollenGrasses: number | null;
  pollenMugwort: number | null;
  pollenRagweed: number | null;
  pollenHazel: number | null;
  pollenAlder: number | null;
  pollenAsh: number | null;
  temperature: number | null;
  humidity: number | null;
  pm25: number | null;
  ozone: number | null;
  asthmaRiskIndex: number | null;
  biowetterRisk: string | null;
  validDate: string;
  dataSource: string;
}

export interface AsthmaEntry {
  id?: number;
  loggedAt?: string;
  symptoms: string;
  severityScore: number;
  peakFlowLMin: number | null;
  inhalerUsage: 'NONE' | 'RESCUE' | 'CONTROLLER';
  notes: string;
}

@Injectable({ providedIn: 'root' })
export class AsthmaService {
  private readonly base = apiUrl('/asthma');

  constructor(private http: HttpClient) {}

  getEnvironment(regionId?: number): Observable<BioWeatherDto> {
    let params = new HttpParams();
    if (regionId != null) params = params.set('regionId', regionId.toString());
    return this.http.get<BioWeatherDto>(`${this.base}/environment`, { params });
  }

  getEntries(): Observable<AsthmaEntry[]> {
    return this.http.get<AsthmaEntry[]>(`${this.base}/entries`);
  }

  getLast7Days(): Observable<AsthmaEntry[]> {
    return this.http.get<AsthmaEntry[]>(`${this.base}/entries/last7days`);
  }

  createEntry(entry: Partial<AsthmaEntry>): Observable<AsthmaEntry> {
    return this.http.post<AsthmaEntry>(`${this.base}/entries`, entry);
  }

  updateEntry(id: number, entry: Partial<AsthmaEntry>): Observable<AsthmaEntry> {
    return this.http.put<AsthmaEntry>(`${this.base}/entries/${id}`, entry);
  }

  deleteEntry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/entries/${id}`);
  }
}

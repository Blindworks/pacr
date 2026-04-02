import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = apiUrl('/completed-trainings');

export interface CompletedTraining {
  id: number;
  trainingDate: string;
  startTime: string | null;
  uploadDate: string;
  activityName: string | null;
  trainingType: string | null;
  sport: string | null;
  subSport: string | null;
  source: string;
  distanceKm: number | null;
  durationSeconds: number | null;
  movingTimeSeconds: number | null;
  averagePaceSecondsPerKm: number | null;
  averageSpeedKmh: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  elevationGainM: number | null;
  calories: number | null;
  averageCadence: number | null;
  averagePowerWatts: number | null;
  minHeartRate: number | null;
  maxSpeedKmh: number | null;
  maxPowerWatts: number | null;
  normalizedPowerWatts: number | null;
  maxCadence: number | null;
  elevationLossM: number | null;
  minElevationM: number | null;
  maxElevationM: number | null;
  temperatureCelsius: number | null;
  totalLaps: number | null;
  bestLapTimeSeconds: number | null;
  timeInHrZone1Seconds: number | null;
  timeInHrZone2Seconds: number | null;
  timeInHrZone3Seconds: number | null;
  timeInHrZone4Seconds: number | null;
  timeInHrZone5Seconds: number | null;
  deviceManufacturer: string | null;
  deviceProduct: string | null;
}

export interface ActivityStreamDto {
  completedTrainingId: number;
  distancePoints: number[];
  heartRate: (number | null)[] | null;
  altitude: (number | null)[] | null;
  paceSecondsPerKm: (number | null)[] | null;
  cadence: (number | null)[] | null;
  power: (number | null)[] | null;
  hasHeartRate: boolean;
  hasAltitude: boolean;
  hasPace: boolean;
  hasCadence: boolean;
  hasPower: boolean;
}

export interface ActivityVo2Max {
  metricType: string;
  label: string;
  value: number;
  unit: string;
  recordedAt: string | null;
}

export interface GpsStreamDto {
  completedTrainingId: number;
  latlng: [number, number][];
  distance: number[];
  heartRate: (number | null)[] | null;
  paceSecondsPerKm: (number | null)[] | null;
  altitude: (number | null)[] | null;
  hasHeartRate: boolean;
  hasPace: boolean;
  hasAltitude: boolean;
}

export interface ActivityMetrics {
  id: number;
  zonesUnknown: boolean;
  z1Min: number | null;
  z2Min: number | null;
  z3Min: number | null;
  z4Min: number | null;
  z5Min: number | null;
  hrDataCoverage: number | null;
  rawLoad: number | null;
  strain21: number | null;
  trimp: number | null;
  trimpQuality: string | null;
  decouplingPct: number | null;
  decouplingEligible: boolean | null;
  decouplingReason: string | null;
  efficiencyFactor: number | null;
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);

  getActivities(page = 0, size = 20): Observable<CompletedTraining[]> {
    return this.http.get<CompletedTraining[]>(`${BASE}?page=${page}&size=${size}`);
  }

  getByDateRange(startDate: string, endDate: string): Observable<CompletedTraining[]> {
    return this.http.get<CompletedTraining[]>(`${BASE}/by-date-range?startDate=${startDate}&endDate=${endDate}`);
  }

  getById(id: number): Observable<CompletedTraining> {
    return this.http.get<CompletedTraining>(`${BASE}/${id}`);
  }

  getStreams(id: number): Observable<ActivityStreamDto> {
    return this.http.get<ActivityStreamDto>(`${BASE}/${id}/streams`);
  }

  getMetrics(id: number): Observable<ActivityMetrics> {
    return this.http.get<ActivityMetrics>(`${BASE}/${id}/metrics`);
  }

  getVo2MaxByActivity(id: number): Observable<ActivityVo2Max[]> {
    return this.http.get<ActivityVo2Max[]>(apiUrl(`/body-metrics/by-activity/${id}`));
  }

  fetchStreams(id: number): Observable<ActivityStreamDto> {
    return this.http.post<ActivityStreamDto>(`${BASE}/${id}/fetch-streams`, null);
  }

  getGpsStream(id: number): Observable<GpsStreamDto> {
    return this.http.get<GpsStreamDto>(`${BASE}/${id}/gps`);
  }

  uploadActivity(file: File, date: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('date', date);
    return this.http.post(`${BASE}/upload`, formData);
  }
}

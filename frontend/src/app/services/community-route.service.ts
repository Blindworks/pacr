import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

const ROUTES_BASE = apiUrl('/community-routes');
const ATTEMPTS_BASE = apiUrl('/route-attempts');

export interface CommunityRouteDto {
  id: number;
  name: string;
  distanceKm: number;
  elevationGainM: number | null;
  startLatitude: number;
  startLongitude: number;
  creatorUsername: string;
  creatorId: number;
  athleteCount: number;
  recordTimeSeconds: number | null;
  recordHolder: string | null;
  visibility: string;
  createdAt: string;
}

export interface CommunityRouteDetailDto extends CommunityRouteDto {
  gpsTrack: [number, number][];
}

export interface LeaderboardEntryDto {
  rank: number;
  username: string;
  userId: number;
  timeSeconds: number;
  paceSecondsPerKm: number | null;
  date: string | null;
}

export interface RouteAttemptDto {
  id: number;
  routeId: number;
  routeName: string;
  status: string;
  timeSeconds: number | null;
  paceSecondsPerKm: number | null;
  completedAt: string | null;
  leaderboardPosition: number | null;
}

export interface CreateCommunityRouteRequest {
  activityId: number;
  name: string;
  visibility?: string;
}

export interface UpdateCommunityRouteRequest {
  name?: string;
  visibility?: string;
}

@Injectable({ providedIn: 'root' })
export class CommunityRouteService {
  private readonly http = inject(HttpClient);

  shareRoute(req: CreateCommunityRouteRequest): Observable<CommunityRouteDto> {
    return this.http.post<CommunityRouteDto>(ROUTES_BASE, req);
  }

  unshareRoute(id: number): Observable<void> {
    return this.http.delete<void>(`${ROUTES_BASE}/${id}`);
  }

  updateRoute(id: number, req: UpdateCommunityRouteRequest): Observable<CommunityRouteDto> {
    return this.http.put<CommunityRouteDto>(`${ROUTES_BASE}/${id}`, req);
  }

  getNearbyRoutes(lat: number, lon: number, radiusKm = 10, sortBy = 'distance',
                  page = 0, size = 20): Observable<CommunityRouteDto[]> {
    const params = new HttpParams()
      .set('lat', lat)
      .set('lon', lon)
      .set('radiusKm', radiusKm)
      .set('sortBy', sortBy)
      .set('page', page)
      .set('size', size);
    return this.http.get<CommunityRouteDto[]>(`${ROUTES_BASE}/nearby`, { params });
  }

  getRouteDetail(id: number): Observable<CommunityRouteDetailDto> {
    return this.http.get<CommunityRouteDetailDto>(`${ROUTES_BASE}/${id}`);
  }

  getMyRoutes(): Observable<CommunityRouteDto[]> {
    return this.http.get<CommunityRouteDto[]>(`${ROUTES_BASE}/mine`);
  }

  getLeaderboard(routeId: number, period = 'ALL_TIME'): Observable<LeaderboardEntryDto[]> {
    const params = new HttpParams().set('period', period);
    return this.http.get<LeaderboardEntryDto[]>(`${ROUTES_BASE}/${routeId}/leaderboard`, { params });
  }

  selectRoute(routeId: number): Observable<RouteAttemptDto> {
    return this.http.post<RouteAttemptDto>(ATTEMPTS_BASE, { routeId });
  }

  cancelPendingAttempt(): Observable<void> {
    return this.http.delete<void>(`${ATTEMPTS_BASE}/pending`);
  }

  getPendingAttempt(): Observable<RouteAttemptDto | null> {
    return this.http.get<RouteAttemptDto | null>(`${ATTEMPTS_BASE}/pending`);
  }

  getAttemptResult(id: number): Observable<RouteAttemptDto> {
    return this.http.get<RouteAttemptDto>(`${ATTEMPTS_BASE}/${id}`);
  }

  getMyAttempts(): Observable<RouteAttemptDto[]> {
    return this.http.get<RouteAttemptDto[]>(`${ATTEMPTS_BASE}/my`);
  }
}

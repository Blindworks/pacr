import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = apiUrl('/admin');

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  blockedUsers: number;
  pendingVerification: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  stravaConnected: number;
  asthmaTrackingEnabled: number;
  cycleTrackingEnabled: number;
  paceZonesConfigured: number;
}

export interface AuditLogEntry {
  id: number;
  timestamp: string;
  actorId: number | null;
  actorUsername: string | null;
  action: string;
  targetType: string | null;
  targetId: string | null;
  details: string | null;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${BASE}/stats`);
  }

  getAuditLog(params: {
    page: number;
    size: number;
    action?: string;
    from?: string;
    to?: string;
  }): Observable<AuditLogPage> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size);
    if (params.action) httpParams = httpParams.set('action', params.action);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);

    return this.http.get<AuditLogPage>(`${BASE}/audit-log`, { params: httpParams });
  }
}

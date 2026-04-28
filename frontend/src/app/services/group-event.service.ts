import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

const BASE = apiUrl('/group-events');

export interface GroupEventDto {
  id: number;
  title: string;
  description: string | null;
  eventDate: string;
  startTime: string;
  endTime: string | null;
  locationName: string;
  latitude: number | null;
  longitude: number | null;
  distanceKm: number | null;
  paceMinSecondsPerKm: number | null;
  paceMaxSecondsPerKm: number | null;
  maxParticipants: number | null;
  currentParticipants: number;
  costCents: number | null;
  costCurrency: string;
  difficulty: string | null;
  status: string;
  trainerUsername: string;
  trainerId: number;
  createdAt: string;
  isRegistered: boolean;
  rrule: string | null;
  recurrenceEndDate: string | null;
  occurrenceDate: string | null;
  isRecurring: boolean;
}

@Injectable({ providedIn: 'root' })
export class GroupEventService {
  private readonly http = inject(HttpClient);

  getNearbyEvents(lat: number, lon: number, radiusKm: number): Observable<GroupEventDto[]> {
    const params = new HttpParams()
      .set('lat', lat.toString())
      .set('lon', lon.toString())
      .set('radiusKm', radiusKm.toString());
    return this.http.get<GroupEventDto[]>(`${BASE}/nearby`, { params });
  }

  getUpcomingEvents(): Observable<GroupEventDto[]> {
    return this.http.get<GroupEventDto[]>(`${BASE}/upcoming`);
  }

  getEventDetail(id: number): Observable<GroupEventDto> {
    return this.http.get<GroupEventDto>(`${BASE}/${id}`);
  }

  registerForEvent(eventId: number, occurrenceDate?: string): Observable<void> {
    let params = new HttpParams();
    if (occurrenceDate) {
      params = params.set('occurrenceDate', occurrenceDate);
    }
    return this.http.post<void>(`${BASE}/${eventId}/register`, {}, { params });
  }

  cancelRegistration(eventId: number, occurrenceDate?: string): Observable<void> {
    let params = new HttpParams();
    if (occurrenceDate) {
      params = params.set('occurrenceDate', occurrenceDate);
    }
    return this.http.delete<void>(`${BASE}/${eventId}/register`, { params });
  }

  getMyRegistrations(): Observable<GroupEventDto[]> {
    return this.http.get<GroupEventDto[]>(`${BASE}/my-registrations`);
  }

  getNearbyForFeed(lat: number, lon: number, radiusKm = 25, days = 30): Observable<GroupEventDto[]> {
    const params = new HttpParams()
      .set('lat', lat.toString())
      .set('lon', lon.toString())
      .set('radiusKm', radiusKm.toString())
      .set('days', days.toString());
    return this.http.get<GroupEventDto[]>(apiUrl('/news-feed/trainer-events'), { params });
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';
import { GroupEventDto } from './group-event.service';

const BASE = apiUrl('/trainer/events');

export interface CreateGroupEventRequest {
  title: string;
  description?: string;
  eventDate: string;
  startTime: string;
  endTime?: string;
  locationName: string;
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
  paceMinSecondsPerKm?: number;
  paceMaxSecondsPerKm?: number;
  maxParticipants?: number;
  costCents?: number;
  costCurrency?: string;
  difficulty?: string;
}

export interface UpdateGroupEventRequest {
  title?: string;
  description?: string;
  eventDate?: string;
  startTime?: string;
  endTime?: string;
  locationName?: string;
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
  paceMinSecondsPerKm?: number;
  paceMaxSecondsPerKm?: number;
  maxParticipants?: number;
  costCents?: number;
  costCurrency?: string;
  difficulty?: string;
}

export interface GroupEventRegistrationDto {
  id: number;
  eventId: number;
  eventTitle: string;
  userId: number;
  username: string;
  status: string;
  registeredAt: string;
}

@Injectable({ providedIn: 'root' })
export class TrainerEventService {
  private readonly http = inject(HttpClient);

  createEvent(request: CreateGroupEventRequest): Observable<GroupEventDto> {
    return this.http.post<GroupEventDto>(BASE, request);
  }

  updateEvent(id: number, request: UpdateGroupEventRequest): Observable<GroupEventDto> {
    return this.http.put<GroupEventDto>(`${BASE}/${id}`, request);
  }

  publishEvent(id: number): Observable<GroupEventDto> {
    return this.http.put<GroupEventDto>(`${BASE}/${id}/publish`, {});
  }

  cancelEvent(id: number): Observable<void> {
    return this.http.put<void>(`${BASE}/${id}/cancel`, {});
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  getTrainerEvents(): Observable<GroupEventDto[]> {
    return this.http.get<GroupEventDto[]>(BASE);
  }

  getParticipants(eventId: number): Observable<GroupEventRegistrationDto[]> {
    return this.http.get<GroupEventRegistrationDto[]>(`${BASE}/${eventId}/participants`);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/users';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;
  heightCm: number | null;
  weightKg: number | null;
  maxHeartRate: number | null;
  hrRest: number | null;
  gender: string | null;
  status: string | null;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string | null;
  heightCm?: number | null;
  weightKg?: number | null;
  maxHeartRate?: number | null;
  hrRest?: number | null;
  gender?: string | null;
  status?: string | null;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${BASE}/me`);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${BASE}/${id}`, request);
  }
}

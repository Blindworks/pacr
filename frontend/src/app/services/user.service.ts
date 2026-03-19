import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { SKIP_AUTH_LOGOUT } from '../interceptors/auth.interceptor';

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
  dwdRegionId: number | null;
  asthmaTrackingEnabled: boolean;
  role: string | null;
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
  dwdRegionId?: number | null;
  asthmaTrackingEnabled?: boolean;
  role?: string | null;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  /** Shared reactive user state — updated after getMe() and updateUser(). */
  readonly currentUser = signal<UserProfile | null>(null);

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${BASE}/me`).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${BASE}/${id}`, request).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  getAllUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(BASE);
  }

  getUserById(id: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${BASE}/${id}`);
  }

  updateUserAsAdmin(id: number, request: UpdateUserRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${BASE}/${id}`, request);
  }

  uploadProfileImage(id: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${BASE}/${id}/profile-image`, formData);
  }

  getProfileImage(id: number): Observable<Blob> {
    return this.http.get(`${BASE}/${id}/profile-image`, {
      responseType: 'blob',
      context: new HttpContext().set(SKIP_AUTH_LOGOUT, true)
    });
  }
}

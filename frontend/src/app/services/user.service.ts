import { apiUrl } from '../core/api-base';
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { SKIP_AUTH_LOGOUT } from '../interceptors/auth.interceptor';

const BASE = apiUrl('/users');

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
  cycleTrackingEnabled: boolean;
  communityRoutesEnabled: boolean;
  groupEventsEnabled: boolean;
  discoverableByOthers: boolean;
  latitude?: number | null;
  longitude?: number | null;
  locationUpdatedAt?: string | null;
  theme: string;
  role: string | null;
  subscriptionPlan: string | null;
  subscriptionExpiresAt: string | null;
  lastLoginAt: string | null;
  onboardingCompleted: boolean;
  targetDistance: string | null;
  weeklyVolumeKm: string | null;
  createdAt?: string | null;
  isBot?: boolean;
  paceRefDistanceM?: number | null;
  paceRefTimeSeconds?: number | null;
  paceRefLabel?: string | null;
  thresholdPaceSecPerKm?: number | null;
  /** Comma-separated ISO 639-1 language codes (e.g. "de,en") used for news feed filtering. */
  preferredNewsLanguages?: string | null;
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
  cycleTrackingEnabled?: boolean;
  communityRoutesEnabled?: boolean;
  groupEventsEnabled?: boolean;
  discoverableByOthers?: boolean;
  role?: string | null;
  subscriptionPlan?: string | null;
  subscriptionExpiresAt?: string | null;
  targetDistance?: string | null;
  weeklyVolumeKm?: string | null;
  theme?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  /** Shared reactive user state — updated after getMe() and updateUser(). */
  readonly currentUser = signal<UserProfile | null>(null);

  clearUser(): void {
    this.currentUser.set(null);
  }

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

  deleteUser(id: number, confirmUsername: string): Observable<void> {
    return this.http.request<void>('delete', `${BASE}/${id}`, {
      body: { confirmUsername }
    });
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

  completeOnboarding(): Observable<UserProfile> {
    return this.http.put<UserProfile>(apiUrl('/users/me/complete-onboarding'), {}).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  setupOnboardingPlan(planId: number, startDate: string, competitionId?: number): Observable<any> {
    return this.http.post(apiUrl('/users/me/onboarding-plan-setup'), { planId, startDate, competitionId });
  }

  updateLocation(latitude: number, longitude: number): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${BASE}/me/location`, { latitude, longitude });
  }

  /**
   * Replaces the user's news-language preference list.
   * Backend stores them comma-separated; we send an array and receive {preferredNewsLanguages: string[]}.
   */
  updateNewsLanguages(languages: string[]): Observable<{ preferredNewsLanguages: string[] }> {
    return this.http.put<{ preferredNewsLanguages: string[] }>(
      `${BASE}/me/news-languages`,
      { languages }
    );
  }

  clearLocation(): Observable<UserProfile> {
    return this.http.delete<UserProfile>(`${BASE}/me/location`);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${BASE}/me/password`, { currentPassword, newPassword });
  }

  getProfileImage(id: number): Observable<Blob | null> {
    return this.http.get(`${BASE}/${id}/profile-image`, {
      responseType: 'blob',
      context: new HttpContext().set(SKIP_AUTH_LOGOUT, true)
    }).pipe(map(blob => blob && blob.size > 0 ? blob : null));
  }
}

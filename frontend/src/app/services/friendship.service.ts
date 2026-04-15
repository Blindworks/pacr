import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

const BASE = apiUrl('/friendships');

export type FriendshipStatusValue = 'NONE' | 'PENDING_OUT' | 'PENDING_IN' | 'FRIENDS';

export interface UserSearchResult {
  id: number;
  username: string;
  displayName: string;
  profileImageFilename: string | null;
  friendshipStatus: FriendshipStatusValue;
  friendshipId: number | null;
  distanceKm?: number | null;
}

export interface Friendship {
  id: number;
  otherUser: UserSearchResult;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'BLOCKED';
  createdAt: string;
  respondedAt: string | null;
  direction: 'INCOMING' | 'OUTGOING';
}

export interface FriendActivity {
  activityId: number;
  friendId: number;
  friendUsername: string;
  friendDisplayName: string;
  profileImageFilename: string | null;
  activityType: string;
  title: string;
  date: string;
  startTime: string | null;
  distanceKm: number | null;
  durationSeconds: number | null;
  sport: string | null;
  averagePaceSecondsPerKm: number | null;
  averageHeartRate: number | null;
  elevationGainM: number | null;
  calories: number | null;
  startLatitude: number | null;
  startLongitude: number | null;
  previewTrack?: [number, number][] | null;
}

export interface LiveTrainingFriend {
  friendId: number;
  username: string;
  displayName: string;
  profileImageFilename: string | null;
  trainingTitle: string | null;
  trainingType: string | null;
  durationMinutes: number | null;
  workPace: string | null;
}

@Injectable({ providedIn: 'root' })
export class FriendshipService {
  private readonly http = inject(HttpClient);

  search(query: string): Observable<UserSearchResult[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<UserSearchResult[]>(`${BASE}/search`, { params });
  }

  searchNearby(lat: number, lon: number, radiusKm: number): Observable<UserSearchResult[]> {
    const params = new HttpParams()
      .set('lat', String(lat))
      .set('lon', String(lon))
      .set('radiusKm', String(radiusKm));
    return this.http.get<UserSearchResult[]>(`${BASE}/search/nearby`, { params });
  }

  listFriends(): Observable<Friendship[]> {
    return this.http.get<Friendship[]>(BASE);
  }

  listIncoming(): Observable<Friendship[]> {
    return this.http.get<Friendship[]>(`${BASE}/incoming`);
  }

  listOutgoing(): Observable<Friendship[]> {
    return this.http.get<Friendship[]>(`${BASE}/outgoing`);
  }

  sendRequest(addresseeId: number): Observable<Friendship> {
    return this.http.post<Friendship>(BASE, { addresseeId });
  }

  accept(id: number): Observable<Friendship> {
    return this.http.post<Friendship>(`${BASE}/${id}/accept`, {});
  }

  decline(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/decline`, {});
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  getActivity(): Observable<FriendActivity[]> {
    return this.http.get<FriendActivity[]>(`${BASE}/activity`);
  }

  getLiveTraining(): Observable<LiveTrainingFriend[]> {
    return this.http.get<LiveTrainingFriend[]>(`${BASE}/live-training`);
  }
}

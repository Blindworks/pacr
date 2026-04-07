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
  friendId: number;
  friendUsername: string;
  friendDisplayName: string;
  profileImageFilename: string | null;
  activityType: string;
  title: string;
  date: string;
  distanceKm: number | null;
  durationSeconds: number | null;
  sport: string | null;
}

@Injectable({ providedIn: 'root' })
export class FriendshipService {
  private readonly http = inject(HttpClient);

  search(query: string): Observable<UserSearchResult[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<UserSearchResult[]>(`${BASE}/search`, { params });
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
}

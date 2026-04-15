import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

export interface ActivityKudos {
  count: number;
  hasKudos: boolean;
}

export interface ActivityComment {
  id: number;
  userId: number | null;
  username: string | null;
  displayName: string | null;
  profileImageFilename: string | null;
  content: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ActivitySocialService {
  private readonly http = inject(HttpClient);

  private base(activityId: number): string {
    return apiUrl(`/activities/${activityId}`);
  }

  getKudos(activityId: number): Observable<ActivityKudos> {
    return this.http.get<ActivityKudos>(`${this.base(activityId)}/kudos`);
  }

  toggleKudos(activityId: number): Observable<ActivityKudos> {
    return this.http.post<ActivityKudos>(`${this.base(activityId)}/kudos`, {});
  }

  listComments(activityId: number): Observable<ActivityComment[]> {
    return this.http.get<ActivityComment[]>(`${this.base(activityId)}/comments`);
  }

  addComment(activityId: number, content: string): Observable<ActivityComment> {
    return this.http.post<ActivityComment>(`${this.base(activityId)}/comments`, { content });
  }

  deleteComment(activityId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(activityId)}/comments/${commentId}`);
  }
}

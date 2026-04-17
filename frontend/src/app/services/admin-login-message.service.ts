import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type LoginMessageTargetType = 'ALL' | 'GROUPS' | 'USERS';
export type LoginMessageTargetGroup = 'PRO' | 'FREE' | 'TRAINER';

export interface UserSummary {
  id: number;
  username: string;
  email: string;
}

export interface LoginMessage {
  id: number;
  title: string;
  content: string;
  published: boolean;
  publishedAt: string | null;
  createdAt: string;
  targetType: LoginMessageTargetType;
  targetGroups: LoginMessageTargetGroup[];
  targetUsers: UserSummary[];
}

export interface CreateLoginMessageRequest {
  title: string;
  content: string;
  targetType: LoginMessageTargetType;
  targetGroups: LoginMessageTargetGroup[];
  targetUserIds: number[];
}

@Injectable({ providedIn: 'root' })
export class AdminLoginMessageService {
  private readonly http = inject(HttpClient);
  private readonly base = apiUrl('/admin/login-messages');

  getAll(): Observable<LoginMessage[]> {
    return this.http.get<LoginMessage[]>(this.base);
  }

  create(data: CreateLoginMessageRequest): Observable<LoginMessage> {
    return this.http.post<LoginMessage>(this.base, data);
  }

  update(id: number, data: CreateLoginMessageRequest): Observable<LoginMessage> {
    return this.http.put<LoginMessage>(`${this.base}/${id}`, data);
  }

  publish(id: number): Observable<LoginMessage> {
    return this.http.post<LoginMessage>(`${this.base}/${id}/publish`, {});
  }

  unpublish(id: number): Observable<LoginMessage> {
    return this.http.post<LoginMessage>(`${this.base}/${id}/unpublish`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginMessage {
  id: number;
  title: string;
  content: string;
  published: boolean;
  publishedAt: string | null;
  createdAt: string;
}

export interface CreateLoginMessageRequest {
  title: string;
  content: string;
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

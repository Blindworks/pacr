import { apiUrl } from '../core/api-base';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type FeedbackCategory = 'BUG' | 'FEATURE_REQUEST' | 'GENERAL';
export type FeedbackStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface Feedback {
  id: number;
  userId: number;
  username: string;
  category: FeedbackCategory;
  subject: string;
  message: string;
  status: FeedbackStatus;
  adminNotes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFeedbackRequest {
  category: FeedbackCategory;
  subject: string;
  message: string;
}

export interface UpdateFeedbackRequest {
  status: FeedbackStatus;
  adminNotes: string | null;
}

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private readonly http = inject(HttpClient);

  submit(data: CreateFeedbackRequest): Observable<Feedback> {
    return this.http.post<Feedback>(apiUrl('/feedback'), data);
  }

  getAll(status?: FeedbackStatus): Observable<Feedback[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Feedback[]>(apiUrl('/admin/feedback'), { params });
  }

  getById(id: number): Observable<Feedback> {
    return this.http.get<Feedback>(apiUrl(`/admin/feedback/${id}`));
  }

  update(id: number, data: UpdateFeedbackRequest): Observable<Feedback> {
    return this.http.put<Feedback>(apiUrl(`/admin/feedback/${id}`), data);
  }
}

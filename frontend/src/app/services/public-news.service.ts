import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

const BASE = apiUrl('/news');

export interface PublicNews {
  id: number;
  title: string;
  content: string;
  excerpt: string | null;
  topicTag: string | null;
  heroImageFilename: string | null;
  isFeatured: boolean;
  isPublished: boolean;
  publishedAt: string | null;
  createdAt: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  hasLiked: boolean;
  isTrending: boolean;
}

export interface TrendingTopic {
  tag: string;
  viewCount: number;
  newsCount: number;
  headline: string | null;
}

export interface NewsComment {
  id: number;
  userId: number | null;
  username: string | null;
  displayName: string | null;
  profileImageFilename: string | null;
  content: string;
  createdAt: string;
  canDelete: boolean;
}

export interface NewsLikeState {
  likeCount: number;
  hasLiked: boolean;
}

@Injectable({ providedIn: 'root' })
export class PublicNewsService {
  private readonly http = inject(HttpClient);

  listPublished(): Observable<PublicNews[]> {
    return this.http.get<PublicNews[]>(BASE);
  }

  getFeatured(): Observable<PublicNews | null> {
    return this.http.get<PublicNews | null>(`${BASE}/featured`);
  }

  get(id: number): Observable<PublicNews> {
    return this.http.get<PublicNews>(`${BASE}/${id}`);
  }

  recordView(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/view`, {});
  }

  getTrending(): Observable<TrendingTopic[]> {
    return this.http.get<TrendingTopic[]>(`${BASE}/trending`);
  }

  getTrendingNews(limit: number = 3): Observable<PublicNews[]> {
    return this.http.get<PublicNews[]>(`${BASE}/trending-news?limit=${limit}`);
  }

  toggleLike(id: number): Observable<NewsLikeState> {
    return this.http.post<NewsLikeState>(`${BASE}/${id}/like`, {});
  }

  getComments(id: number): Observable<NewsComment[]> {
    return this.http.get<NewsComment[]>(`${BASE}/${id}/comments`);
  }

  addComment(id: number, content: string): Observable<NewsComment> {
    return this.http.post<NewsComment>(`${BASE}/${id}/comments`, { content });
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/comments/${commentId}`);
  }
}

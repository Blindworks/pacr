import { apiUrl } from '../core/api-base';
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { SKIP_AUTH_LOGOUT } from '../interceptors/auth.interceptor';
import { UserService } from './user.service';
import { ThemeService } from './theme.service';

const TOKEN_KEY = 'auth_token';
const ROLE_KEY = 'auth_role';
const BASE = apiUrl('/auth');

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  role: string;
  status: string;
}

export interface MessageResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly userService = inject(UserService);
  private readonly themeService = inject(ThemeService);
  private _isLoggedIn = signal(!!localStorage.getItem(TOKEN_KEY));

  get isLoggedIn() {
    return this._isLoggedIn.asReadonly();
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${BASE}/login`, { username: email, password }).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(ROLE_KEY, res.role);
        this._isLoggedIn.set(true);
      })
    );
  }

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${BASE}/register`, { username, email, password });
  }

  verifyEmail(email: string, code: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${BASE}/verify-email`, { email, code }, {
      context: new HttpContext().set(SKIP_AUTH_LOGOUT, true)
    });
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${BASE}/resend-verification`, { email }, {
      context: new HttpContext().set(SKIP_AUTH_LOGOUT, true)
    });
  }

  logout() {
    const token = this.getToken();

    // Clear local state immediately
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    this._isLoggedIn.set(false);
    this.userService.clearUser();
    this.themeService.setTheme('dark');

    // Fire-and-forget: blacklist token on backend (use captured token directly)
    if (token) {
      this.http.post(`${BASE}/logout`, {}, {
        headers: new HttpHeaders({ Authorization: `Bearer ${token}` }),
        context: new HttpContext().set(SKIP_AUTH_LOGOUT, true)
      }).subscribe({ error: () => {} });
    }
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getRole(): string | null {
    return localStorage.getItem(ROLE_KEY);
  }
}

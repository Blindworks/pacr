import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'auth_token';
const BASE = 'http://localhost:8080/api/auth';

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  role: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private _isLoggedIn = signal(!!localStorage.getItem(TOKEN_KEY));

  get isLoggedIn() {
    return this._isLoggedIn.asReadonly();
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${BASE}/login`, { username: email, password }).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this._isLoggedIn.set(true);
      })
    );
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    this._isLoggedIn.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }
}

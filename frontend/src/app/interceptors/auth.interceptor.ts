import { HttpInterceptorFn, HttpErrorResponse, HttpContextToken, HttpContext } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const SKIP_AUTH_LOGOUT = new HttpContextToken<boolean>(() => false);

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const skipLogout = req.context.get(SKIP_AUTH_LOGOUT);

  const token = auth.getToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !skipLogout && !isRefreshing) {
        const refreshToken = auth.getRefreshToken();
        if (refreshToken) {
          isRefreshing = true;
          return auth.refreshAccessToken().pipe(
            switchMap(res => {
              isRefreshing = false;
              const retryReq = req.clone({
                setHeaders: { Authorization: `Bearer ${res.token}` }
              });
              return next(retryReq);
            }),
            catchError(refreshErr => {
              isRefreshing = false;
              auth.logout();
              router.navigate(['/login']);
              return throwError(() => refreshErr);
            })
          );
        }
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};

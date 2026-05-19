import {
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthService } from '../service/auth.service';

const ACCESS_TOKEN_HEADER = 'X-Access-Token';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  const request = token
    ? req.clone({
        withCredentials: true,
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      })
    : req.clone({
        withCredentials: true,
      });

  return next(request).pipe(
    tap((event) => {
      if (!(event instanceof HttpResponse)) {
        return;
      }

      const refreshedAccessToken = event.headers.get(ACCESS_TOKEN_HEADER);
      if (refreshedAccessToken) {
        authService.updateAccessToken(refreshedAccessToken);
      }
    }),
  );
};

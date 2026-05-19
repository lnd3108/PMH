import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, finalize, map, of, tap, throwError } from 'rxjs';
import {
  LoginRequest,
  LoginResponse,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
} from '../models/auth.models';
import { ApiResponse } from './category.service';

const AUTH_STORAGE_KEY = 'paymenthub.auth';
const ACCESS_TOKEN_HEADER = 'X-Access-Token';

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: MeResponse;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = 'http://localhost:8094/api/jpa/auth';
  private readonly sessionState = signal<AuthSession | null>(this.readSession());

  readonly session = this.sessionState.asReadonly();
  readonly user = computed(() => this.sessionState()?.user ?? null);
  readonly isAuthenticated = computed(() => !!this.sessionState()?.accessToken);

  login(payload: LoginRequest) {
    return this.http
      .post<ApiResponse<LoginResponse> | LoginResponse>(`${this.apiBaseUrl}/login`, payload, {
        observe: 'response',
        withCredentials: true,
      })
      .pipe(
        map((response) => this.extractLoginPayload(response)),
        tap((session) => this.persistSession(session)),
      );
  }

  register(payload: RegisterRequest) {
    return this.http.post<ApiResponse<RegisterResponse> | RegisterResponse>(
      `${this.apiBaseUrl}/register`,
      payload,
      {
        withCredentials: true,
      },
    );
  }

  logout() {
    return this.http
      .post(
        `${this.apiBaseUrl}/logout`,
        {},
        {
          observe: 'response',
          withCredentials: true,
        },
      )
      .pipe(
        catchError((error) => {
          this.clearSession();
          return throwError(() => error);
        }),
        finalize(() => {
          this.clearSession();
        }),
      );
  }

  getAccessToken(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  updateAccessToken(accessToken: string): void {
    const current = this.sessionState();
    if (!current || !accessToken || current.accessToken === accessToken) {
      return;
    }

    const nextSession: AuthSession = {
      ...current,
      accessToken,
    };

    this.sessionState.set(nextSession);
    sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession));
  }

  clearSession(): void {
    this.sessionState.set(null);
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
  }

  private extractLoginPayload(
    response: HttpResponse<ApiResponse<LoginResponse> | LoginResponse>,
  ): AuthSession {
    const body = response.body;
    if (!body) {
      throw new Error('Login response body is empty.');
    }

    const payload = 'data' in body ? body.data : body;
    const accessToken = payload.accessToken || response.headers.get(ACCESS_TOKEN_HEADER);

    if (!accessToken) {
      throw new Error('Access token is missing in login response.');
    }

    return {
      accessToken,
      tokenType: payload.tokenType ?? 'Bearer',
      expiresIn: payload.expiresIn ?? 300,
      user: payload.user,
    };
  }

  private persistSession(session: AuthSession): void {
    this.sessionState.set(session);
    sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  }

  private readSession(): AuthSession | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }

    const rawSession = sessionStorage.getItem(AUTH_STORAGE_KEY);
    if (!rawSession) {
      return null;
    }

    try {
      return JSON.parse(rawSession) as AuthSession;
    } catch {
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
  }
}

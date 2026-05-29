import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError, tap, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    tenantId: string;
    tenantName?: string;
    tenantLogoDataUrl?: string | null;
    legalName?: string;
    roles: string[];
  };
  redirectUrl?: string;
}

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  tenantId: string;
  tenantName?: string;
  tenantLogoDataUrl?: string | null;
  legalName?: string;
  roles: string[];
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly apiUrl = `${environment.apiUrl}/api`;
  private readonly tokenKey = 'auth_token';
  private readonly refreshTokenKey = 'refresh_token';
  private readonly userKey = 'auth_user';

  // Signal API per reactive state
  readonly isAuthenticated = signal(this.hasValidToken());
  readonly currentUser = signal<AuthUser | null>(this.getStoredUser());
  readonly isLoading = signal(false);
  readonly error = signal('');

  /**
   * Login con email e password
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    this.isLoading.set(true);
    this.error.set('');

    return this.http.post<LoginResponse>(
      `${this.apiUrl}/public/auth/login`,
      credentials
    ).pipe(
      tap((response) => {
        this.handleAuthSuccess(response);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        const errorMessage = this.getErrorMessage(error);
        this.error.set(errorMessage);
        return throwError(() => ({ message: errorMessage }));
      })
    );
  }

  /**
   * Refresh token
   */
  refreshAccessToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => ({ message: 'No refresh token available' }));
    }

    return this.http.post<LoginResponse>(
      `${this.apiUrl}/public/auth/refresh`,
      { refreshToken }
    ).pipe(
      tap((response) => {
        this.saveTokens(response.accessToken, response.refreshToken);
        this.isAuthenticated.set(true);
      }),
      catchError((error) => {
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout
   */
  logout(): void {
    this.clearTokens();
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
    this.error.set('');
    this.router.navigate(['/public/login']);
  }

  /**
   * Cambia password dell'utente autenticato
   */
  changePassword(payload: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/staff/account/change-password`, payload).pipe(
      catchError((error) => {
        const errorMessage = this.getErrorMessage(error);
        return throwError(() => ({ message: errorMessage }));
      })
    );
  }

  refreshCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${this.apiUrl}/staff/account/me`).pipe(
      tap((user) => {
        this.saveUser(user);
        this.currentUser.set(user);
      }),
      catchError((error) => {
        const errorMessage = this.getErrorMessage(error);
        return throwError(() => ({ message: errorMessage }));
      })
    );
  }

  /**
   * Ottieni token attuale
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  /**
   * Ottieni refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  /**
   * Verifica se autenticato
   */
  isAuthenticatedSync(): boolean {
    return this.isAuthenticated();
  }

  /**
   * Ottieni utente corrente
   */
  getCurrentUser(): AuthUser | null {
    return this.currentUser();
  }

  hasAnyRole(...roles: string[]): boolean {
    const userRoles = this.currentUser()?.roles ?? [];
    return roles.some((role) => userRoles.includes(role));
  }

  hasSuperAdminAccess(): boolean {
    return this.hasAnyRole('SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'MANAGER', 'ROLE_MANAGER');
  }

  getDefaultRouteForCurrentUser(): string {
    return this.hasSuperAdminAccess() ? '/admin/dashboard' : '/staff/dashboard';
  }

  /**
   * Gestisci successo autenticazione
   */
  private handleAuthSuccess(response: LoginResponse): void {
    this.saveTokens(response.accessToken, response.refreshToken);
    this.saveUser(response.user);
    this.currentUser.set(response.user);
    this.isAuthenticated.set(true);
    this.isLoading.set(false);
  }

  /**
   * Salva token in localStorage
   */
  private saveTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.tokenKey, accessToken);
    localStorage.setItem(this.refreshTokenKey, refreshToken);
  }

  /**
   * Salva utente in localStorage
   */
  private saveUser(user: AuthUser): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  /**
   * Ottieni utente da localStorage
   */
  private getStoredUser(): AuthUser | null {
    const stored = localStorage.getItem(this.userKey);
    return stored ? JSON.parse(stored) : null;
  }

  /**
   * Pulisci token
   */
  private clearTokens(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userKey);
  }

  /**
   * Verifica se token valido
   */
  private hasValidToken(): boolean {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) return false;

    // Decodifica JWT per verificare scadenza
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  /**
   * Estrai messaggio errore
   */
  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.error?.message) {
      return error.error.message;
    }

    switch (error.status) {
      case 401:
        return 'Email o password non corretti';
      case 403:
        return 'Account disabilitato o sospeso';
      case 404:
        return 'Utente non trovato';
      case 500:
        return 'Errore del server. Riprova più tardi';
      default:
        return 'Errore durante il login. Riprova';
    }
  }
}

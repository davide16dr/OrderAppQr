import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Auth JWT Interceptor (Angular 21+)
 * Aggiunge automaticamente il token JWT agli header delle richieste HTTP
 */
export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();
  const tenantId = resolveTenantId(authService, token);
  const traceEnabled = shouldTraceTenant(req);

  if (traceEnabled) {
    console.info('[auth-interceptor] Outgoing request', {
      method: req.method,
      url: req.url,
      hasToken: !!token,
      resolvedTenantId: tenantId
    });
  }

  // Clonare la request e aggiungere il token se disponibile
  if (token && isAuthUrl(req)) {
    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`
    };

    if (tenantId) {
      headers['X-Tenant-Id'] = tenantId;
    }

    if (shouldSetJsonContentType(req)) {
      headers['Content-Type'] = 'application/json';
    }

    req = req.clone({
      setHeaders: headers
    });
  } else if (traceEnabled) {
    console.warn('[auth-interceptor] Request sent without auth header', {
      method: req.method,
      url: req.url,
      reason: token ? 'URL excluded by isAuthUrl' : 'missing token'
    });
  }

  return next(req);
};

function resolveTenantId(authService: AuthService, token: string | null): string | null {
  const currentUserTenantId = authService.getCurrentUser()?.tenantId;
  if (currentUserTenantId) {
    return String(currentUserTenantId);
  }

  const storedUserTenantId = getTenantIdFromStoredUser();
  if (storedUserTenantId) {
    return storedUserTenantId;
  }

  if (!token) {
    return null;
  }

  return extractTenantIdFromJwt(token);
}

function shouldSetJsonContentType(req: HttpRequest<unknown>): boolean {
  if (req.headers.has('Content-Type')) {
    return false;
  }

  if (req.method === 'GET' || req.method === 'HEAD') {
    return false;
  }

  const body = req.body as unknown;
  if (body == null) {
    return false;
  }

  if (typeof FormData !== 'undefined' && body instanceof FormData) {
    return false;
  }

  return true;
}

function extractTenantIdFromJwt(token: string): string | null {
  try {
    const payloadPart = token.split('.')[1];
    if (!payloadPart) {
      return null;
    }

    const payloadJson = base64UrlDecode(payloadPart);
    const payload = JSON.parse(payloadJson) as {
      tenantId?: string | number;
      tenant_id?: string | number;
      tid?: string | number;
    };

    const tenantId = payload.tenantId ?? payload.tenant_id ?? payload.tid;
    return tenantId != null ? String(tenantId) : null;
  } catch {
    return null;
  }
}

function getTenantIdFromStoredUser(): string | null {
  try {
    const stored = localStorage.getItem('auth_user');
    if (!stored) {
      return null;
    }

    const parsed = JSON.parse(stored) as { tenantId?: string | number };
    return parsed.tenantId != null ? String(parsed.tenantId) : null;
  } catch {
    return null;
  }
}

function base64UrlDecode(input: string): string {
  const normalized = input.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
  return atob(padded);
}

/**
 * Verificare se l'URL richiede autenticazione
 * Non aggiungere token agli endpoint pubblici
 */
function isAuthUrl(req: HttpRequest<unknown>): boolean {
  return req.url.includes('/api/') && 
         !req.url.includes('/public/');
}

function shouldTraceTenant(req: HttpRequest<unknown>): boolean {
  return req.url.includes('/api/dashboard') || req.url.includes('/api/staff/stations');
}

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Auth Guard moderno (Angular 21+)
 * Protegge le rotte richiedendo autenticazione valida
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Verificare se autenticato
  if (authService.isAuthenticatedSync()) {
    return true;
  }

  return router.createUrlTree(['/public/login'], {
    queryParams: { returnUrl: state.url }
  });
};

/**
 * Subscription Guard
 * Blocca l'accesso alle rotte staff se l'abbonamento è scaduto.
 * Reindirizza alla pagina abbonamento dove l'utente può rinnovare.
 */
export const subscriptionGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();
  if (!user) return true;

  if (user.isDemo || authService.hasSuperAdminAccess()) return true;

  const status = user.subscriptionStatus;
  // undefined/null means we don't know yet — don't block, trust the backend to set NONE/EXPIRED/CANCELLED explicitly
  const blocked = status === 'EXPIRED' || status === 'CANCELLED' || status === 'NONE';
  if (blocked) return router.createUrlTree(['/staff/settings']);

  if (status === 'TRIAL' && user.trialEndsAt && new Date(user.trialEndsAt) < new Date()) {
    return router.createUrlTree(['/staff/settings']);
  }

  return true;
};

/**
 * Public Guard
 * Reindirizza gli autenticati che provano ad accedere alle pagine pubbliche (login, signup)
 */
export const publicGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Gli utenti demo possono sempre accedere alle pagine pubbliche (login, signup)
  if (authService.isAuthenticatedSync() && !authService.currentUser()?.isDemo) {
    return router.createUrlTree([authService.getDefaultRouteForCurrentUser()]);
  }

  return true;
};

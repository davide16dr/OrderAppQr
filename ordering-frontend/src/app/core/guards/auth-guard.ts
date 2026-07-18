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

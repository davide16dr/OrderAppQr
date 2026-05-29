import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export const superAdminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasSuperAdminAccess()) {
    return true;
  }

  // Redirect to the home page if not a super admin
  router.navigate(['/']);
  return false;
};

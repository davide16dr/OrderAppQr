import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export const superAdminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();
  const roles = user?.roles ?? [];
  if (roles.includes('SUPER_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
    return true;
  }

  // Redirect to the home page if not a super admin
  router.navigate(['/']);
  return false;
};

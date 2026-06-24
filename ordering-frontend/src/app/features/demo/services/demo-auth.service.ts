import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService, AuthUser } from '../../../core/services/auth.service';

export const DEMO_USER: AuthUser = {
  id: 'demo-1',
  email: 'demo@lido-bellissimo.it',
  firstName: 'Demo',
  lastName: 'Staff',
  tenantId: '999',
  tenantName: 'Lido Bellissimo',
  tenantLogoDataUrl: null,
  roles: ['MANAGER'],
};

@Injectable()
export class DemoAuthService extends AuthService {
  private readonly nav = inject(Router);

  constructor() {
    super();
    this.currentUser.set(DEMO_USER);
    this.isAuthenticated.set(true);
  }

  override logout(): void {
    this.nav.navigate(['/']);
  }

  override refreshCurrentUser(): Observable<AuthUser> {
    return of(DEMO_USER);
  }

  override getCurrentUser(): AuthUser | null {
    return DEMO_USER;
  }
}

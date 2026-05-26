import { Routes } from '@angular/router';
import { BusinessSignupComponent } from './pages/business-signup/business-signup.component';
import { SignupSuccessComponent } from './pages/signup-success/signup-success.component';
import { StaffLoginComponent } from './pages/staff-login/staff-login.component';
import { publicGuard } from '../../core/guards/auth-guard';

export const PUBLIC_ROUTES: Routes = [
  {
    path: 'business-signup',
    component: BusinessSignupComponent,
    canActivate: [publicGuard]
  },
  {
    path: 'signup-success',
    component: SignupSuccessComponent,
    canActivate: [publicGuard]
  },
  {
    path: 'login',
    component: StaffLoginComponent,
    canActivate: [publicGuard]
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];

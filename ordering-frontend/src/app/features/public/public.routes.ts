import { Routes } from '@angular/router';
import { BusinessSignupComponent } from './pages/business-signup/business-signup.component';
import { SignupSuccessComponent } from './pages/signup-success/signup-success.component';
import { StaffLoginComponent } from './pages/staff-login/staff-login.component';
import { PaymentSuccessComponent } from './pages/payment-success/payment-success.component';
import { PaymentCancelComponent } from './pages/payment-cancel/payment-cancel.component';
import { DemoLandingComponent } from './pages/demo-landing/demo-landing.component';
import { PrivacyPolicyComponent } from './pages/privacy-policy/privacy-policy.component';
import { TerminiComponent } from './pages/termini/termini.component';
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
  { path: 'demo',            component: DemoLandingComponent },
  { path: 'privacy',         component: PrivacyPolicyComponent },
  { path: 'termini',         component: TerminiComponent },
  { path: 'payment/success', component: PaymentSuccessComponent },
  { path: 'payment/cancel',  component: PaymentCancelComponent  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];

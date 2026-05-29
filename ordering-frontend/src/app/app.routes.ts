import { Routes } from '@angular/router';
import { HomeComponent } from './features/customer/pages/home/home';
import { Cart } from './features/customer/pages/cart/cart';
import { LocationMenu } from './features/customer/pages/location-menu/location-menu';
import { OrderConfirmation } from './features/customer/pages/order-confirmation/order-confirmation';
import { PUBLIC_ROUTES } from './features/public/public.routes';
import { authGuard } from './core/guards/auth-guard';
import { AdminGlobalCatalog } from './features/admin/pages/admin-global-catalog/admin-global-catalog';
import { STAFF_ROUTES } from './features/staff/staff.routes';
import { superAdminGuard } from './features/admin/guards/super-admin.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  {
    path: 'customer',
    children: [
      { path: 'menu', component: LocationMenu },
      { path: 'cart', component: Cart },
      { path: 'order', component: OrderConfirmation },
      { path: '', redirectTo: 'menu', pathMatch: 'full' },
    ],
  },
  { path: 'public', children: PUBLIC_ROUTES },
  {
    path: 'staff',
    canActivate: [authGuard],
    children: STAFF_ROUTES,
  },
  {
    path: 'admin/menu',
    component: AdminGlobalCatalog,
    canActivate: [superAdminGuard],
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
    canActivate: [superAdminGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];

import { Routes } from '@angular/router';
import { StaffLayoutComponent } from './layout/staff-layout/staff-layout.component';
import { DashboardPageComponent } from './pages/dashboard-page/dashboard-page.component';
import { OrdersBoardPageComponent } from './pages/orders-board-page/orders-board-page.component';
import { AllOrdersPageComponent } from './pages/all-orders-page/all-orders-page.component';
import { MenuPageComponent } from './pages/menu-page/menu-page.component';
import { StationsPageComponent } from './pages/stations-page/stations-page.component';
import { StatisticsPageComponent } from './pages/statistics-page/statistics-page.component';
import { SettingsPageComponent } from './pages/settings-page/settings-page.component';
import { subscriptionGuard } from '../../core/guards/auth-guard';

export const STAFF_ROUTES: Routes = [
  {
    path: '',
    component: StaffLayoutComponent,
    children: [
      { path: 'dashboard',  component: DashboardPageComponent,  canActivate: [subscriptionGuard] },
      { path: 'orders',     component: OrdersBoardPageComponent, canActivate: [subscriptionGuard] },
      { path: 'all-orders', component: AllOrdersPageComponent,   canActivate: [subscriptionGuard] },
      { path: 'menu',       component: MenuPageComponent,        canActivate: [subscriptionGuard] },
      { path: 'stations',   component: StationsPageComponent,    canActivate: [subscriptionGuard] },
      { path: 'statistics', component: StatisticsPageComponent,  canActivate: [subscriptionGuard] },
      { path: 'settings',   component: SettingsPageComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];

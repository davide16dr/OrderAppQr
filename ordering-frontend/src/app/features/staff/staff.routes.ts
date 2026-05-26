import { Routes } from '@angular/router';
import { StaffLayoutComponent } from './layout/staff-layout/staff-layout.component';
import { DashboardPageComponent } from './pages/dashboard-page/dashboard-page.component';
import { OrdersBoardPageComponent } from './pages/orders-board-page/orders-board-page.component';
import { AllOrdersPageComponent } from './pages/all-orders-page/all-orders-page.component';
import { MenuPageComponent } from './pages/menu-page/menu-page.component';
import { StationsPageComponent } from './pages/stations-page/stations-page.component';
import { StatisticsPageComponent } from './pages/statistics-page/statistics-page.component';
import { SettingsPageComponent } from './pages/settings-page/settings-page.component';

export const STAFF_ROUTES: Routes = [
  {
    path: '',
    component: StaffLayoutComponent,
    children: [
      { path: 'dashboard', component: DashboardPageComponent },
      { path: 'orders', component: OrdersBoardPageComponent },
      { path: 'all-orders', component: AllOrdersPageComponent },
      { path: 'menu', component: MenuPageComponent },
      { path: 'stations', component: StationsPageComponent },
      { path: 'statistics', component: StatisticsPageComponent },
      { path: 'settings', component: SettingsPageComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];

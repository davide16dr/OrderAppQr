import { Routes } from '@angular/router';
import { DEMO_MODE } from './demo.tokens';
import { DemoStateService } from './services/demo-state.service';
import { DemoAuthService } from './services/demo-auth.service';
import { DemoDashboardService } from './services/demo-dashboard.service';
import { DemoOrderEventsWsService } from './services/demo-order-events-ws.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../staff/services/dashboard.service';
import { OrderEventsWsService } from '../staff/services/order-events-ws.service';
import { StaffLayoutComponent } from '../staff/layout/staff-layout/staff-layout.component';
import { DashboardPageComponent } from '../staff/pages/dashboard-page/dashboard-page.component';
import { OrdersBoardPageComponent } from '../staff/pages/orders-board-page/orders-board-page.component';
import { AllOrdersPageComponent } from '../staff/pages/all-orders-page/all-orders-page.component';
import { MenuPageComponent } from '../staff/pages/menu-page/menu-page.component';
import { StationsPageComponent } from '../staff/pages/stations-page/stations-page.component';
import { StatisticsPageComponent } from '../staff/pages/statistics-page/statistics-page.component';
import { SettingsPageComponent } from '../staff/pages/settings-page/settings-page.component';

export const DEMO_ROUTES: Routes = [
  {
    path: '',
    component: StaffLayoutComponent,
    providers: [
      { provide: DEMO_MODE, useValue: true },
      DemoStateService,
      { provide: AuthService, useClass: DemoAuthService },
      { provide: DashboardService, useClass: DemoDashboardService },
      { provide: OrderEventsWsService, useClass: DemoOrderEventsWsService },
    ],
    children: [
      { path: 'dashboard', component: DashboardPageComponent },
      { path: 'orders', component: OrdersBoardPageComponent },
      { path: 'all-orders', component: AllOrdersPageComponent },
      { path: 'menu', component: MenuPageComponent },
      { path: 'stations', component: StationsPageComponent },
      { path: 'statistics', component: StatisticsPageComponent },
      { path: 'settings', component: SettingsPageComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
];

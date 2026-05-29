import { Route } from '@angular/router';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';
import { TenantListPageComponent } from './pages/tenant-list/tenant-list.page';
import { TenantCreatePageComponent } from './pages/tenant-create/tenant-create.page';
import { TenantEditPageComponent } from './pages/tenant-edit/tenant-edit.page';

export const ADMIN_ROUTES: Route[] = [
  {
    path: 'dashboard',
    component: AdminDashboard,
  },
  {
    path: 'tenants',
    component: TenantListPageComponent,
  },
  {
    path: 'tenants/new',
    component: TenantCreatePageComponent,
  },
  {
    path: 'tenants/:id/edit',
    component: TenantEditPageComponent,
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
];

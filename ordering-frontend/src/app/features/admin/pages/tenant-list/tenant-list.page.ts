import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminTenantService, Tenant } from '../../services/admin-tenant.service';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-tenant-list-page',
  templateUrl: './tenant-list.page.html',
  styleUrls: ['./tenant-list.page.scss'],
  standalone: true,
  imports: [CommonModule, DatePipe],
})
export class TenantListPageComponent implements OnInit {
  readonly adminTenantService = inject(AdminTenantService);
  readonly authService = inject(AuthService);
  readonly router = inject(Router);

  tenants = this.adminTenantService.tenants;
  loading = this.adminTenantService.loading;
  error = this.adminTenantService.error;
  searchTerm = signal('');
  selectedTenant = signal<Tenant | null>(null);

  ngOnInit(): void {
    this.adminTenantService.loadTenants();
  }

  filteredTenants = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const tenants = this.tenants();
    if (!term) return tenants;
    return tenants.filter(t =>
      t.name.toLowerCase().includes(term) || t.slug.toLowerCase().includes(term)
    );
  });

  activeCount = computed(() => this.tenants().filter(t => t.enabled).length);

  onToggleTenant(tenantId: number, currentEnabled: boolean): void {
    this.adminTenantService.updateTenantStatusAndRefresh(tenantId, !currentEnabled);
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
  }

  openInfo(tenant: Tenant): void {
    this.selectedTenant.set(tenant);
  }

  closeInfo(): void {
    this.selectedTenant.set(null);
  }

  onEditTenant(tenantId: number): void {
    this.router.navigate([`/admin/tenants/${tenantId}/edit`]);
    this.closeInfo();
  }

  onLogout(): void {
    this.authService.logout();
  }

  initials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map(w => w[0] ?? '')
      .join('')
      .toUpperCase();
  }

  planLabel(plan: string | null | undefined): string {
    if (!plan) return 'Non impostato';
    const map: Record<string, string> = { FREE: 'Free', PRO: 'Pro', ENTERPRISE: 'Enterprise' };
    return map[plan.toUpperCase()] ?? plan;
  }
}

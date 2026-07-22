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
  detailLoading = signal(false);

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
    // se il panel è aperto aggiorna il campo enabled in tempo reale
    const cur = this.selectedTenant();
    if (cur?.id === tenantId) {
      this.selectedTenant.set({ ...cur, enabled: !currentEnabled });
    }
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
  }

  openInfo(tenant: Tenant): void {
    this.selectedTenant.set(tenant);
    this.detailLoading.set(true);
    this.adminTenantService.getTenant(tenant.id).subscribe({
      next: (full) => {
        this.selectedTenant.set(full);
        this.detailLoading.set(false);
      },
      error: () => {
        this.detailLoading.set(false);
      }
    });
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
    const map: Record<string, string> = {
      FREE: 'Free', BASIC: 'Basic', STARTER: 'Starter',
      PRO: 'Pro', ENTERPRISE: 'Enterprise'
    };
    return map[plan.toUpperCase()] ?? plan;
  }

  renewLoading = signal(false);
  expireLoading = signal(false);

  onExpireSubscription(tenantId: number): void {
    this.expireLoading.set(true);
    this.adminTenantService.expireSubscription(tenantId).subscribe({
      next: () => {
        this.expireLoading.set(false);
        this.adminTenantService.loadTenants();
        const cur = this.selectedTenant();
        if (cur?.id === tenantId) this.openInfo(cur);
      },
      error: () => { this.expireLoading.set(false); }
    });
  }

  onRenewManually(tenantId: number, billingCycle: 'MONTHLY' | 'YEARLY' = 'MONTHLY'): void {
    this.renewLoading.set(true);
    this.adminTenantService.renewManually(tenantId, billingCycle).subscribe({
      next: () => {
        this.renewLoading.set(false);
        this.adminTenantService.loadTenants();
        const cur = this.selectedTenant();
        if (cur?.id === tenantId) this.openInfo(cur);
      },
      error: () => { this.renewLoading.set(false); }
    });
  }

  subStatusLabel(status: string | null | undefined): string {
    const map: Record<string, string> = {
      ACTIVE: 'Attivo', TRIAL: 'In prova', PENDING: 'In attesa',
      PAST_DUE: 'Scaduto', EXPIRED: 'Scaduto', CANCELLED: 'Cancellato', SUSPENDED: 'Sospeso'
    };
    return status ? (map[status] ?? status) : '—';
  }

  subStatusColor(status: string | null | undefined): string {
    if (!status) return '';
    if (status === 'ACTIVE') return '#16a34a';
    if (status === 'PAST_DUE' || status === 'SUSPENDED') return '#dc2626';
    if (status === 'PENDING') return '#d97706';
    return '#6b7280';
  }

  payStatusLabel(status: string | null | undefined): string {
    const map: Record<string, string> = {
      PAID: 'Pagato', PENDING: 'In attesa', FAILED: 'Fallito',
      REFUNDED: 'Rimborsato', NONE: '—'
    };
    return status ? (map[status] ?? status) : '—';
  }

  payStatusColor(status: string | null | undefined): string {
    if (!status) return '';
    if (status === 'PAID') return '#16a34a';
    if (status === 'FAILED') return '#dc2626';
    if (status === 'PENDING') return '#d97706';
    return '#6b7280';
  }
}

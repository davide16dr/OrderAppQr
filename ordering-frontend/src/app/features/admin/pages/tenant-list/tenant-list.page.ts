import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminTenantService } from '../../services/admin-tenant.service';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-tenant-list-page',
  templateUrl: './tenant-list.page.html',
  styleUrls: ['./tenant-list.page.scss'],
  standalone: true,
  imports: [CommonModule],
})
export class TenantListPageComponent {
  readonly adminTenantService = inject(AdminTenantService);
  readonly authService = inject(AuthService);
  readonly router = inject(Router);

  // Esponiamo direttamente i signals del service
  tenants = this.adminTenantService.tenants;
  loading = this.adminTenantService.loading;
  error = this.adminTenantService.error;
  searchTerm = signal('');

  filteredTenants = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const tenants = this.tenants();

    if (!term) {
      return tenants;
    }

    return tenants.filter((tenant) => {
      return (
        tenant.name.toLowerCase().includes(term) ||
        tenant.slug.toLowerCase().includes(term)
      );
    });
  });

  onToggleTenant(tenantId: number, currentEnabled: boolean): void {
    // Aggiornamento reattivo istantaneo
    this.adminTenantService.updateTenantStatusAndRefresh(tenantId, !currentEnabled);
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
  }

  onEditTenant(tenantId: number) {
    this.router.navigate([`/admin/tenants/${tenantId}/edit`]);
  }

  onCreateTenant() {
    this.router.navigate(['/admin/tenants/new']);
  }

  onLogout(): void {
    this.authService.logout();
  }
}

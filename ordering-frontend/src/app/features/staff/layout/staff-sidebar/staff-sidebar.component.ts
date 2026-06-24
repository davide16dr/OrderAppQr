import { ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthUser } from '../../../../core/services/auth.service';

interface StaffNavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-staff-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './staff-sidebar.component.html',
  styleUrl: './staff-sidebar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StaffSidebarComponent {
  @Input() currentUser: AuthUser | null = null;
  @Input() mobileOpen = false;
  @Input() routePrefix = '/staff';
  @Output() logout = new EventEmitter<void>();
  @Output() navigate = new EventEmitter<void>();

  @HostBinding('class.mobile-open') get mobileOpenClass(): boolean {
    return this.mobileOpen;
  }

  get navItems(): StaffNavItem[] {
    return [
      { label: 'Dashboard',    icon: '📊', route: `${this.routePrefix}/dashboard`  },
      { label: 'Ordini',       icon: '📋', route: `${this.routePrefix}/orders`     },
      { label: 'Tutti Ordini', icon: '🗂️', route: `${this.routePrefix}/all-orders` },
      { label: 'Menu',         icon: '🍽️', route: `${this.routePrefix}/menu`       },
      { label: 'Postazioni',   icon: '🪑', route: `${this.routePrefix}/stations`   },
      { label: 'Statistiche',  icon: '📈', route: `${this.routePrefix}/statistics` },
    ];
  }

  get settingsRoute(): string {
    return `${this.routePrefix}/settings`;
  }

  get initials(): string {
    if (!this.currentUser) {
      return 'S';
    }
    return `${this.currentUser.firstName[0] || ''}${this.currentUser.lastName[0] || ''}`.toUpperCase();
  }

  get tenantDisplayName(): string {
    const user = this.currentUser as (AuthUser & { tenantName?: string; legalName?: string }) | null;
    if (!user) {
      return 'Ordering SaaS';
    }

    const byTenantName = user.tenantName?.trim();
    if (byTenantName) {
      return byTenantName;
    }

    const byLegalName = user.legalName?.trim();
    if (byLegalName) {
      return byLegalName;
    }

    const tenantId = user.tenantId?.trim();
    if (tenantId) {
      return `Tenant ${tenantId}`;
    }

    return 'Ordering SaaS';
  }

  get tenantLogoDataUrl(): string | null {
    const user = this.currentUser as (AuthUser & { tenantLogoDataUrl?: string | null }) | null;
    return user?.tenantLogoDataUrl ?? null;
  }

  onLogout(): void {
    this.logout.emit();
  }

  onNavigate(): void {
    this.navigate.emit();
  }
}

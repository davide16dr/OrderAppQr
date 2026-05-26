import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthUser } from '../../../../core/services/auth.service';
import { DashboardSection } from '../../models/dashboard-section';

interface NavItem {
  id: DashboardSection;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-sidebar.component.html',
  styleUrls: ['./dashboard-sidebar.component.scss']
})
export class DashboardSidebarComponent {
  @Input() activeSection: DashboardSection = 'overview';
  @Input() currentUser: AuthUser | null = null;
  @Output() sectionChange = new EventEmitter<DashboardSection>();
  @Output() logout = new EventEmitter<void>();

  navItems: NavItem[] = [
    { id: 'overview', label: 'Dashboard', icon: '📊' },
    { id: 'orders', label: 'Ordini', icon: '📋' },
    { id: 'revenue', label: 'Fatturato', icon: '💰' },
    { id: 'products', label: 'Prodotti', icon: '🥤' },
    { id: 'areas', label: 'Aree', icon: '📍' },
    { id: 'stations', label: 'Postazioni', icon: '🪑' }
  ];

  onNavClick(section: DashboardSection): void {
    this.sectionChange.emit(section);
  }

  onLogout(): void {
    this.logout.emit();
  }

  get businessName(): string {
    const user = this.currentUser as (AuthUser & { tenantName?: string; legalName?: string }) | null;
    if (!user) return 'Dashboard';

    const byTenantName = user.tenantName?.trim();
    if (byTenantName) return byTenantName;

    const byLegalName = user.legalName?.trim();
    if (byLegalName) return byLegalName;

    const tenantId = user.tenantId?.trim();
    if (tenantId) return `Tenant ${tenantId}`;

    return 'Dashboard';
  }

  get businessInitial(): string {
    const name = this.businessName.trim();
    return name ? name.charAt(0).toUpperCase() : 'D';
  }

  getUserInitials(): string {
    if (!this.currentUser) return 'U';
    return `${this.currentUser.firstName.charAt(0)}${this.currentUser.lastName.charAt(0)}`.toUpperCase();
  }
}

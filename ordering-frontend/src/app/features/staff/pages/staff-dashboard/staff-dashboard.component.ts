import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../../../core/services/auth.service';
import { DashboardSidebarComponent } from '../../components/dashboard-sidebar/dashboard-sidebar.component';
import { StaffDashboardMainComponent } from '../../components/staff-dashboard-main/staff-dashboard-main.component';
import { DashboardSection } from '../../models/dashboard-section';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardSidebarComponent,
    StaffDashboardMainComponent
  ],
  templateUrl: './staff-dashboard.component.html',
  styleUrl: './staff-dashboard.component.scss',
  animations: []
})
export class StaffDashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = signal<AuthUser | null>(null);
  readonly isLoading = signal(true);
  readonly activeSection = signal<DashboardSection>('overview');

  ngOnInit(): void {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    const user = this.authService.getCurrentUser();
    
    if (!user) {
      this.router.navigate(['/public/login']);
      return;
    }

    this.currentUser.set(user);
    this.isLoading.set(false);
  }

  onSectionChange(section: DashboardSection): void {
    this.activeSection.set(section);
  }

  logout(): void {
    this.authService.logout();
  }

  get isAdmin(): boolean {
    const user = this.currentUser();
    return user?.roles.includes('ADMIN') ?? false;
  }

  get staffName(): string {
    const user = this.currentUser();
    return user ? `${user.firstName} ${user.lastName}` : 'Staff Member';
  }
}

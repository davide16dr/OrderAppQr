import { Component, Input } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { AuthUser } from '../../../../core/services/auth.service';
import { DashboardSection } from '../../models/dashboard-section';
import { DashboardMainComponent } from '../dashboard-main/dashboard-main.component';
import { OrdersByHourComponent } from '../orders-by-hour/orders-by-hour.component';
import { WeeklyRevenueComponent } from '../weekly-revenue/weekly-revenue.component';
import { TopProductsComponent } from '../top-products/top-products.component';
import { AreaManagementComponent } from '../area-management/area-management.component';
import { StationManagementComponent } from '../station-management/station-management.component';
@Component({
  selector: 'app-staff-dashboard-main',
  standalone: true,
  imports: [
    DashboardMainComponent,
    OrdersByHourComponent,
    WeeklyRevenueComponent,
    TopProductsComponent,
    AreaManagementComponent,
    StationManagementComponent
  ],
  templateUrl: './staff-dashboard-main.component.html',
  styleUrl: './staff-dashboard-main.component.scss',
  animations: [
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(20px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
      ])
    ])
  ]
})
export class StaffDashboardMainComponent {
  @Input({ required: true }) activeSection!: DashboardSection;
  @Input({ required: true }) isLoading!: boolean;
  @Input({ required: true }) currentUser!: AuthUser | null;

  refreshCount = 0;

  refresh(): void {
    this.refreshCount++;
  }

  get sectionTitle(): string {
    const titles: Record<DashboardSection, string> = {
      overview:     'Dashboard',
      orders:       'Ordini per Ora',
      revenue:      'Fatturato Settimanale',
      products:     'Menu',
      areas:        'Aree',
      stations:     'Postazioni',
      subscription: ''
    };
    return titles[this.activeSection] ?? 'Dashboard';
  }
}

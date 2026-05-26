import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
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
    CommonModule,
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
}

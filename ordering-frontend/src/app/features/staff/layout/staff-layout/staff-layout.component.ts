import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { StaffSidebarComponent } from '../staff-sidebar/staff-sidebar.component';
import { StaffTopbarComponent } from '../staff-topbar/staff-topbar.component';

@Component({
  selector: 'app-staff-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, StaffSidebarComponent, StaffTopbarComponent],
  templateUrl: './staff-layout.component.html',
  styleUrl: './staff-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StaffLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);

  readonly currentUser = this.authService.currentUser;

  ngOnInit(): void {
    this.authService.refreshCurrentUser().subscribe({
      error: () => {
        // Keep the locally stored profile if the refresh endpoint is unavailable.
      },
    });
  }

  onLogout(): void {
    this.authService.logout();
  }
}

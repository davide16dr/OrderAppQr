import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthUser } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-staff-topbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './staff-topbar.component.html',
  styleUrl: './staff-topbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StaffTopbarComponent {
  @Input() currentUser: AuthUser | null = null;

  get initials(): string {
    if (!this.currentUser) {
      return 'S';
    }
    return `${this.currentUser.firstName[0] || ''}${this.currentUser.lastName[0] || ''}`.toUpperCase();
  }

  refreshCurrentPage(): void {
    window.dispatchEvent(new CustomEvent('staff-refresh-requested'));
  }
}

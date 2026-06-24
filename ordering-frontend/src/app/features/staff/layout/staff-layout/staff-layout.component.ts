import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import { StaffSidebarComponent } from '../staff-sidebar/staff-sidebar.component';
import { StaffTopbarComponent } from '../staff-topbar/staff-topbar.component';
import { OrderEventsWsService } from '../../services/order-events-ws.service';
import { OrderNotificationService } from '../../../../core/services/order-notification.service';
import { DEMO_MODE } from '../../../demo/demo.tokens';

interface OrderToast {
  id: string;
  orderId: number;
}

@Component({
  selector: 'app-staff-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, StaffSidebarComponent, StaffTopbarComponent],
  templateUrl: './staff-layout.component.html',
  styleUrl: './staff-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.demo-active]': 'isDemoMode' }
})
export class StaffLayoutComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly orderEventsWs = inject(OrderEventsWsService);
  private readonly orderNotification = inject(OrderNotificationService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly isDemoMode = inject(DEMO_MODE, { optional: true }) ?? false;
  readonly currentUser = this.authService.currentUser;
  readonly isSidebarOpen = signal(false);
  readonly toasts = signal<OrderToast[]>([]);

  ngOnInit(): void {
    this.authService.refreshCurrentUser().subscribe({
      error: () => {},
    });

    this.orderEventsWs.ensureConnected();

    this.orderEventsWs.events$
      .pipe(filter(e => e.eventType === 'ORDER_CREATED'), takeUntil(this.destroy$))
      .subscribe(event => {
        this.orderNotification.playNewOrder();
        const toast: OrderToast = { id: crypto.randomUUID(), orderId: event.orderId };
        this.toasts.update(t => [...t, toast]);
        setTimeout(() => this.dismissToast(toast.id), 6000);
      });
  }

  dismissToast(id: string): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }

  exitDemo(): void {
    this.router.navigate(['/']);
  }

  toggleSidebar(): void {
    this.isSidebarOpen.update((value) => !value);
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
  }

  onLogout(): void {
    this.authService.logout();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

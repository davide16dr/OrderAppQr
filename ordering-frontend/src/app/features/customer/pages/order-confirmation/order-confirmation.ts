import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { CustomerOrderEventsWsService } from '../../services/customer-order-events-ws.service';
import { CustomerOrder } from '../../services/customer-order';
import { CustomerOrderItem, formatEuroFromCents } from '../../models/customer.types';

@Component({
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-confirmation.html',
  styleUrl: './order-confirmation.scss',
})
export class OrderConfirmation implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly customerOrder = inject(CustomerOrder);
  private readonly orderEventsWs = inject(CustomerOrderEventsWsService);
  private readonly eventsSubscription = new Subscription();
  private readonly destroy$ = new Subject<void>();
  readonly order = this.customerOrder.currentOrder;
  readonly steps = ['Ricevuto', 'Consegnato'] as const;

  ngOnInit(): void {
    const orderId = this.resolveOrderId();
    if (orderId === null) {
      return;
    }

    this.orderEventsWs.connect(orderId);
    this.eventsSubscription.add(
      this.orderEventsWs.events$.subscribe((event) => {
        if (event.eventType !== 'STATUS_CHANGED' || event.status.toUpperCase() !== 'DELIVERED') {
          return;
        }

        this.markDelivered();
      })
    );

    timer(0, 5000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.customerOrder.getOrderStatus(String(orderId)))
      )
      .subscribe((status) => {
        if (status === 'DELIVERED') {
          this.markDelivered();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.eventsSubscription.unsubscribe();
    this.orderEventsWs.disconnect();
  }

  readonly activeIndex = computed(() => {
    const status = this.order()?.status;
    if (!status) return 0;
    return status === 'DELIVERED' ? 1 : 0;
  });

  title = computed(() => {
    const status = this.order()?.status;
    if (status === 'DELIVERED') return 'Consegnato';
    return 'Ricevuto';
  });

  subtitle = computed(() => {
    const status = this.order()?.status;
    if (status === 'DELIVERED') return 'Grazie!';
    return 'Il tuo ordine è stato ricevuto';
  });

  euro(cents: number): string {
    return formatEuroFromCents(cents);
  }

  newOrder(): void {
    this.customerOrder.clear();
    const context = this.customerOrder.getOrderContext();
    this.router.navigate(['/customer/menu'], { queryParams: this.buildMenuQueryParams(context) });
  }

  private buildMenuQueryParams(context: { token: string | null; tenant: string | null; location: string | null }): Record<string, string> {
    return {
      ...(context.token ? { token: context.token } : {}),
      ...(context.tenant ? { tenant: context.tenant } : {}),
      ...(context.location ? { location: context.location } : {}),
    };
  }

  private resolveOrderId(): number | null {
    const rawOrderId = this.order()?.id;
    if (!rawOrderId) {
      return null;
    }

    const parsed = Number.parseInt(rawOrderId, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private markDelivered(): void {
    this.customerOrder.setStatus('DELIVERED');
  }

  trackByItem(_: number, item: CustomerOrderItem): string {
    return item.productId;
  }
}

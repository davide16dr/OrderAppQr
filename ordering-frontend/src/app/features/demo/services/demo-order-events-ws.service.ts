import { Injectable, inject } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { OrderEventsWsService, OrderEventPayload } from '../../staff/services/order-events-ws.service';
import { DemoStateService } from './demo-state.service';

@Injectable()
export class DemoOrderEventsWsService extends OrderEventsWsService {
  private readonly state = inject(DemoStateService);

  private readonly demoSubject = new Subject<OrderEventPayload>();
  override readonly events$: Observable<OrderEventPayload> = this.demoSubject.asObservable();

  private timerId: ReturnType<typeof setTimeout> | null = null;
  private started = false;

  override ensureConnected(): void {
    if (this.started) return;
    this.started = true;
    // First event after 8 seconds, then random 12-20s intervals
    this.scheduleNext(8000);
  }

  private scheduleNext(ms: number): void {
    this.timerId = setTimeout(() => {
      this.fireNewOrder();
      this.scheduleNext(12000 + Math.random() * 8000);
    }, ms);
  }

  private fireNewOrder(): void {
    const order = this.state.createNewOrder();
    this.state.addOrder(order);
    this.demoSubject.next({
      orderId: order.id,
      tenantId: 999,
      status: 'RECEIVED',
      eventType: 'ORDER_CREATED',
    });
  }

  override disconnect(): void {
    if (this.timerId !== null) {
      clearTimeout(this.timerId);
      this.timerId = null;
    }
    this.started = false;
  }

  override ngOnDestroy(): void {
    this.disconnect();
    this.demoSubject.complete();
  }
}

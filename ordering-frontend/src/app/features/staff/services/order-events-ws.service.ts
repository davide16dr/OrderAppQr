import { Injectable, OnDestroy, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';

export interface OrderEventPayload {
  orderId: number;
  tenantId: number;
  status: string;
  eventType: 'ORDER_CREATED' | 'STATUS_CHANGED' | string;
}

@Injectable({
  providedIn: 'root',
})
export class OrderEventsWsService implements OnDestroy {
  private readonly auth = inject(AuthService);

  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private subscribedTenantId: number | null = null;

  private readonly eventsSubject = new Subject<OrderEventPayload>();
  readonly events$: Observable<OrderEventPayload> = this.eventsSubject.asObservable();

  ensureConnected(): void {
    const tenantId = this.resolveTenantId();
    if (!tenantId) {
      return;
    }

    if (this.client && this.subscribedTenantId === tenantId) {
      return;
    }

    this.disconnect();

    const client = new Client({
      webSocketFactory: () => new SockJS(`${environment.apiUrl}/ws`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
    });

    client.onConnect = () => {
      this.subscribedTenantId = tenantId;
      this.subscription = client.subscribe(`/topic/tenant/${tenantId}/orders`, (message: IMessage) => {
        const payload = this.safeParse(message.body);
        if (payload) {
          this.eventsSubject.next(payload);
        }
      });
    };

    client.onStompError = () => {
      // Keep silent: we'll auto-reconnect.
    };

    client.onWebSocketError = () => {
      // Keep silent: we'll auto-reconnect.
    };

    this.client = client;
    client.activate();
  }

  private resolveTenantId(): number | null {
    const user = this.auth.getCurrentUser();
    const parsed = user?.tenantId ? Number.parseInt(String(user.tenantId), 10) : NaN;
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private safeParse(raw: string): OrderEventPayload | null {
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as Partial<OrderEventPayload>;
      if (typeof parsed.orderId !== 'number' || typeof parsed.tenantId !== 'number') {
        return null;
      }
      return {
        orderId: parsed.orderId,
        tenantId: parsed.tenantId,
        status: String(parsed.status ?? ''),
        eventType: String(parsed.eventType ?? ''),
      };
    } catch {
      return null;
    }
  }

  disconnect(): void {
    try {
      this.subscription?.unsubscribe();
    } catch {
      // ignore
    }

    this.subscription = null;
    this.subscribedTenantId = null;

    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // ignore
      }
    }

    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventsSubject.complete();
  }
}

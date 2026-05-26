import { Injectable, OnDestroy, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

export interface CustomerOrderEventPayload {
  orderId: number;
  tenantId: number;
  status: string;
  eventType: 'ORDER_CREATED' | 'STATUS_CHANGED' | string;
}

@Injectable({
  providedIn: 'root',
})
export class CustomerOrderEventsWsService implements OnDestroy {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private subscribedOrderId: number | null = null;

  private readonly eventsSubject = new Subject<CustomerOrderEventPayload>();
  readonly events$: Observable<CustomerOrderEventPayload> = this.eventsSubject.asObservable();

  private readonly authService = inject(AuthService);

  connect(orderId: number): void {
    if (!Number.isFinite(orderId) || orderId <= 0) {
      return;
    }

    if (this.client && this.subscribedOrderId === orderId) {
      return;
    }

    this.disconnect();

    const tenantId = this.authService.getCurrentUser()?.tenantId ?? this.getTenantIdFromStorage();

    // Public pages may carry tenant slug in the URL query params (e.g. ?tenant=emergenza-4)
    const urlParams = new URLSearchParams(window.location.search);
    const tenantSlugFromUrl = urlParams.get('tenant');

    let wsUrl = `${environment.apiUrl}/ws`;
    if (tenantId) {
      wsUrl += `?xTenantId=${tenantId}`;
    } else if (tenantSlugFromUrl) {
      wsUrl += `?tenant=${encodeURIComponent(tenantSlugFromUrl)}`;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
    });

    client.onConnect = () => {
      this.subscribedOrderId = orderId;
      this.subscription = client.subscribe(`/topic/orders/${orderId}`, (message: IMessage) => {
        const payload = this.safeParse(message.body);
        if (payload) {
          this.eventsSubject.next(payload);
        }
      });
    };

    client.onStompError = () => {
      // Keep silent: auto-reconnect handles transient issues.
    };

    client.onWebSocketError = () => {
      // Keep silent: auto-reconnect handles transient issues.
    };

    this.client = client;
    client.activate();
  }

  private safeParse(raw: string): CustomerOrderEventPayload | null {
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as Partial<CustomerOrderEventPayload>;
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

  private getTenantIdFromStorage(): string | null {
    try {
      const stored = localStorage.getItem('auth_user');
      if (!stored) return null;
      const parsed = JSON.parse(stored) as { tenantId?: string | number };
      return parsed.tenantId != null ? String(parsed.tenantId) : null;
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
    this.subscribedOrderId = null;

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
import { Injectable, computed, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { CartLine, CustomerOrderData, OrderStatus } from '../models/customer.types';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CustomerOrder {
  private static readonly ORDER_CONTEXT_KEY = 'orderapp.customer.orderContext';
  private static readonly CURRENT_ORDER_KEY = 'orderapp.customer.currentOrder';

  private readonly currentSignal = signal<CustomerOrderData | null>(null);
  readonly currentOrder = computed(() => this.currentSignal());

  private readonly http = inject(HttpClient);

  constructor() {
    this.hydrateCurrentOrder();
  }

  placeOrder(lines: CartLine[], note: string): Observable<CustomerOrderData> {
    // Try to send to backend; fall back to mock only if the backend is unreachable.
    return this.sendOrderToBackend(lines, note).pipe(
      catchError((error) => {
        const status = Number((error as any)?.status);
        if (status === 0) {
          console.warn('Backend unreachable, using mock order');
          return this.createMockOrder(lines, note);
        }
        throw error;
      })
    );
  }

  private sendOrderToBackend(lines: CartLine[], note: string): Observable<CustomerOrderData> {
    const context = this.resolveOrderContext();

    const payload = {
      token: context.token,
      tenant: context.tenant,
      location: context.location,
      customerNote: note,
      items: lines.map(l => ({
        tenantProductId: parseInt(l.productId, 10),
        productName: l.name,
        quantity: l.quantity,
        unitPriceCents: l.unitPriceCents,
        selectedModifierOptionIds: l.selectedModifierOptionIds ?? []
      }))
    };

    const url = `${environment.apiUrl}/api/public/customer/orders`;

    return this.http.post<{ id: number; status: string; totalAmount: number }>(url, payload).pipe(
      map((response) => {
        const totalCents = Math.round((Number(response?.totalAmount ?? 0)) * 100);
        const mapped: CustomerOrderData = {
          id: String(response?.id ?? this.generateOrderId()),
          status: this.mapBackendStatus(response?.status),
          items: lines.map(l => ({
            productId: l.productId,
            name: l.name,
            quantity: l.quantity,
            unitPriceCents: l.unitPriceCents
          })),
          totalCents
        };
        return mapped;
      }),
      tap(order => this.setCurrentOrder(order)),
      catchError(err => {
        console.error('Error creating order:', err);
        throw err;
      })
    );
  }

  private createMockOrder(lines: CartLine[], note: string): Observable<CustomerOrderData> {
    const id = this.generateOrderId();
    const totalCents = lines.reduce((sum, l) => sum + l.unitPriceCents * l.quantity, 0);
    const order: CustomerOrderData = {
      id,
      status: 'RECEIVED',
      items: lines.map(l => ({
        productId: l.productId,
        name: l.name,
        quantity: l.quantity,
        unitPriceCents: l.unitPriceCents,
      })),
      totalCents,
    };

    this.setCurrentOrder(order);
    return of(order);
  }

  setStatus(status: OrderStatus): void {
    const current = this.currentSignal();
    if (!current) return;
    this.setCurrentOrder({ ...current, status });
  }

  clear(): void {
    this.currentSignal.set(null);
    try {
      sessionStorage.removeItem(CustomerOrder.CURRENT_ORDER_KEY);
    } catch {
      // Ignore storage issues.
    }
  }

  getOrderContext(): { token: string | null; tenant: string | null; location: string | null } {
    return this.resolveOrderContext();
  }

  private generateOrderId(): string {
    // Short human-friendly code.
    return Math.random().toString(36).slice(2, 10).toUpperCase();
  }

  private mapBackendStatus(status: string | undefined): OrderStatus {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'NEW' || normalized === 'ACCEPTED') return 'RECEIVED';
    if (normalized === 'DELIVERED') return 'DELIVERED';
    return 'RECEIVED';
  }

  private resolveOrderContext(): { token: string | null; tenant: string | null; location: string | null } {
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get('token') ?? params.get('station');
    const urlTenant = params.get('tenant');
    const urlLocation = params.get('location');

    let stored: { token?: string | null; tenant?: string | null; location?: string | null } | null = null;
    try {
      const raw = sessionStorage.getItem(CustomerOrder.ORDER_CONTEXT_KEY);
      stored = raw ? JSON.parse(raw) : null;
    } catch {
      stored = null;
    }

    return {
      token: urlToken ?? stored?.token ?? null,
      tenant: urlTenant ?? stored?.tenant ?? null,
      location: urlLocation ?? stored?.location ?? null
    };
  }

  getOrderStatus(orderId: string): Observable<OrderStatus | null> {
    const parsedOrderId = Number.parseInt(orderId, 10);
    if (!Number.isFinite(parsedOrderId) || parsedOrderId <= 0) {
      return of(null);
    }

    return this.http.get<{ status?: string }>(`${environment.apiUrl}/api/public/customer/orders/${parsedOrderId}`).pipe(
      map((response) => this.mapBackendStatus(response?.status)),
      catchError((error) => {
        const status = Number((error as any)?.status);
        if (status === 404) {
          return of(null);
        }
        throw error;
      })
    );
  }

  private setCurrentOrder(order: CustomerOrderData): void {
    this.currentSignal.set(order);
    try {
      sessionStorage.setItem(CustomerOrder.CURRENT_ORDER_KEY, JSON.stringify(order));
    } catch {
      // Ignore storage issues.
    }
  }

  private hydrateCurrentOrder(): void {
    try {
      const raw = sessionStorage.getItem(CustomerOrder.CURRENT_ORDER_KEY);
      if (!raw) {
        return;
      }

      const parsed = JSON.parse(raw) as CustomerOrderData;
      if (parsed && parsed.id && Array.isArray(parsed.items)) {
        this.currentSignal.set(parsed);
      }
    } catch {
      // Ignore invalid persisted state.
    }
  }
}

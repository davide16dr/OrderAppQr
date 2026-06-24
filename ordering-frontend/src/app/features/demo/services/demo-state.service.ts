import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { TenantOrder } from '../../staff/services/dashboard.service';

const now = () => new Date().toISOString();
const minsAgo = (m: number) => new Date(Date.now() - m * 60000).toISOString();

const INITIAL_ORDERS: TenantOrder[] = [
  {
    id: 101, code: 'ORD-0101', locationLabel: 'Ombrellone 14', areaName: 'Zona Spiaggia',
    type: 'BAR', status: 'RECEIVED', note: null, total: 12.50, createdAt: minsAgo(3),
    items: [{ quantity: 2, name: 'Spritz Aperol', total: 10.00 }, { quantity: 1, name: 'Acqua Frizzante', total: 2.50 }]
  },
  {
    id: 102, code: 'ORD-0102', locationLabel: 'Tavolo 3', areaName: 'Bar Interno',
    type: 'KITCHEN', status: 'RECEIVED', note: 'Senza cipolla', total: 13.50, createdAt: minsAgo(8),
    items: [{ quantity: 1, name: 'Margherita', total: 9.00 }, { quantity: 1, name: 'Birra Media', total: 4.50 }]
  },
  {
    id: 103, code: 'ORD-0103', locationLabel: 'Terrazzo 1', areaName: 'Terrazzo',
    type: 'BAR', status: 'RECEIVED', note: null, total: 15.00, createdAt: minsAgo(14),
    items: [{ quantity: 3, name: 'Spritz Aperol', total: 15.00 }]
  },
  {
    id: 104, code: 'ORD-0104', locationLabel: 'Ombrellone 8', areaName: 'Zona Spiaggia',
    type: 'BAR', status: 'DELIVERED', note: null, total: 3.30, createdAt: minsAgo(37),
    items: [{ quantity: 1, name: 'Caffè', total: 1.50 }, { quantity: 1, name: 'Cornetto', total: 1.80 }]
  },
  {
    id: 105, code: 'ORD-0105', locationLabel: 'Bar 1', areaName: 'Bar Interno',
    type: 'BAR', status: 'DELIVERED', note: null, total: 8.00, createdAt: minsAgo(45),
    items: [{ quantity: 1, name: 'Toast', total: 4.50 }, { quantity: 1, name: 'Coca Cola', total: 3.50 }]
  },
  {
    id: 106, code: 'ORD-0106', locationLabel: 'Ombrellone 22', areaName: 'Zona Spiaggia',
    type: 'BAR', status: 'CANCELLED', note: null, total: 4.00, createdAt: minsAgo(62),
    items: [{ quantity: 2, name: 'Acqua Frizzante', total: 4.00 }]
  },
  {
    id: 107, code: 'ORD-0107', locationLabel: 'Tavolo 2', areaName: 'Bar Interno',
    type: 'KITCHEN', status: 'DELIVERED', note: null, total: 13.50, createdAt: minsAgo(90),
    items: [{ quantity: 1, name: 'Patatine Fritte', total: 5.00 }, { quantity: 2, name: 'Birra Media', total: 9.00 }]
  },
  {
    id: 108, code: 'ORD-0108', locationLabel: 'Terrazzo 2', areaName: 'Terrazzo',
    type: 'BAR', status: 'DELIVERED', note: 'Grazie!', total: 24.00, createdAt: minsAgo(120),
    items: [{ quantity: 4, name: 'Spritz Aperol', total: 20.00 }, { quantity: 2, name: 'Acqua Frizzante', total: 4.00 }]
  },
];

const LOCATIONS = ['Ombrellone 5', 'Ombrellone 17', 'Tavolo 1', 'Terrazzo 3', 'Bar 2', 'Ombrellone 31', 'Tavolo 4'];
const AREA_FOR: Record<string, string> = {
  'Ombrellone 5': 'Zona Spiaggia', 'Ombrellone 17': 'Zona Spiaggia', 'Ombrellone 31': 'Zona Spiaggia',
  'Tavolo 1': 'Bar Interno', 'Tavolo 4': 'Bar Interno', 'Bar 2': 'Bar Interno',
  'Terrazzo 3': 'Terrazzo',
};
const NEW_ORDER_ITEMS = [
  [{ quantity: 2, name: 'Spritz Aperol', total: 10.00 }],
  [{ quantity: 1, name: 'Birra Media', total: 4.50 }, { quantity: 1, name: 'Patatine Fritte', total: 5.00 }],
  [{ quantity: 1, name: 'Margherita', total: 9.00 }],
  [{ quantity: 3, name: 'Coca Cola', total: 10.50 }],
  [{ quantity: 1, name: 'Spritz Aperol', total: 5.00 }, { quantity: 1, name: 'Bruschette', total: 6.00 }],
];

@Injectable()
export class DemoStateService {
  private nextId = 109;
  private readonly ordersSubject = new BehaviorSubject<TenantOrder[]>(INITIAL_ORDERS);
  readonly orders$ = this.ordersSubject.asObservable();

  getOrders(): TenantOrder[] {
    return this.ordersSubject.getValue();
  }

  updateOrderStatus(orderId: number, status: string): void {
    const updated = this.ordersSubject.getValue().map(o =>
      o.id === orderId ? { ...o, status } : o
    );
    this.ordersSubject.next(updated);
  }

  addOrder(order: TenantOrder): void {
    this.ordersSubject.next([order, ...this.ordersSubject.getValue()]);
  }

  createNewOrder(): TenantOrder {
    const id = this.nextId++;
    const loc = LOCATIONS[(id - 109) % LOCATIONS.length];
    const items = NEW_ORDER_ITEMS[(id - 109) % NEW_ORDER_ITEMS.length];
    const total = items.reduce((s, it) => s + it.total, 0);
    return {
      id,
      code: `ORD-0${id}`,
      locationLabel: loc,
      areaName: AREA_FOR[loc] ?? 'Zona Spiaggia',
      type: 'BAR',
      status: 'RECEIVED',
      note: null,
      total,
      createdAt: now(),
      items,
    };
  }
}

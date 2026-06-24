import { Injectable, inject } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import {
  DashboardService, DashboardMetrics, TenantProduct, TenantProductDetails,
  TenantOrder, TenantSettings, TenantArea, TenantStationSummary,
  TenantStationStats, TenantStationResponse, TenantStationQr,
  TenantOrderTargetStatus, CreateTenantProductPayload, UpdateTenantProductPayload
} from '../../staff/services/dashboard.service';
import { DemoStateService } from './demo-state.service';

const MOCK_METRICS: DashboardMetrics = {
  totalRevenueToday: 487.50,
  totalOrdersToday: 23,
  activeCustomerCount: 8,
  averageOrderTime: 4,
  lastUpdated: new Date().toISOString(),
  ordersByHour: [
    { hour: 9, count: 2 }, { hour: 10, count: 4 }, { hour: 11, count: 5 },
    { hour: 12, count: 8 }, { hour: 13, count: 6 }, { hour: 14, count: 4 },
    { hour: 15, count: 3 }, { hour: 16, count: 5 }, { hour: 17, count: 7 },
    { hour: 18, count: 4 }, { hour: 19, count: 2 },
  ],
  weeklyRevenue: [
    { day: 'Lun', revenue: 312 }, { day: 'Mar', revenue: 289 },
    { day: 'Mer', revenue: 445 }, { day: 'Gio', revenue: 521 },
    { day: 'Ven', revenue: 634 }, { day: 'Sab', revenue: 789 },
    { day: 'Dom', revenue: 487.50 },
  ],
  topProducts: [
    { id: 1, name: 'Spritz Aperol', quantity: 45 },
    { id: 2, name: 'Birra Media', quantity: 38 },
    { id: 3, name: 'Margherita', quantity: 27 },
    { id: 4, name: 'Coca Cola', quantity: 24 },
    { id: 5, name: 'Patatine Fritte', quantity: 19 },
  ],
  areaDistribution: [
    { areaId: 1, areaName: 'Zona Spiaggia', orderCount: 14 },
    { areaId: 2, areaName: 'Bar Interno', orderCount: 6 },
    { areaId: 3, areaName: 'Terrazzo', orderCount: 3 },
  ],
};

const MOCK_PRODUCTS: TenantProduct[] = [
  { id: 1, name: 'Spritz Aperol', description: 'Classico aperitivo italiano', price: 5.00, imageUrl: null, category: 'Aperitivi', department: 'BAR', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'BAR-001' },
  { id: 2, name: 'Birra Media', description: 'Birra alla spina 33cl', price: 4.50, imageUrl: null, category: 'Birre', department: 'BAR', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'BAR-002' },
  { id: 3, name: 'Coca Cola', description: 'Bibita in lattina', price: 3.50, imageUrl: null, category: 'Bibite', department: 'BAR', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'BAR-003' },
  { id: 4, name: 'Acqua Frizzante', description: 'Bottiglia 50cl', price: 2.00, imageUrl: null, category: 'Bibite', department: 'BAR', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'BAR-004' },
  { id: 5, name: 'Caffè', description: 'Espresso', price: 1.50, imageUrl: null, category: 'Caffetteria', department: 'BAR', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'BAR-005' },
  { id: 6, name: 'Succo di Frutta', description: 'ACE, Pesca o Ananas', price: 3.00, imageUrl: null, category: 'Bibite', department: 'BAR', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 3, extrasCount: 0, sku: 'BAR-006' },
  { id: 7, name: 'Margherita', description: 'Pomodoro e mozzarella', price: 9.00, imageUrl: null, category: 'Pizze', department: 'KITCHEN', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 3, sku: 'KIT-001' },
  { id: 8, name: 'Patatine Fritte', description: 'Porzione abbondante', price: 5.00, imageUrl: null, category: 'Snack', department: 'KITCHEN', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'KIT-002' },
  { id: 9, name: 'Toast', description: 'Prosciutto e formaggio', price: 4.50, imageUrl: null, category: 'Snack', department: 'KITCHEN', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'KIT-003' },
  { id: 10, name: 'Bruschette', description: 'Pomodoro e basilico', price: 6.00, imageUrl: null, category: 'Antipasti', department: 'KITCHEN', vatRate: 10, status: 'ACTIVE', availableForOrder: true, variantsCount: 0, extrasCount: 0, sku: 'KIT-004' },
];

const MOCK_AREAS: TenantArea[] = [
  { id: 1, name: 'Zona Spiaggia', displayOrder: 1, status: 'ACTIVE' },
  { id: 2, name: 'Bar Interno', displayOrder: 2, status: 'ACTIVE' },
  { id: 3, name: 'Terrazzo', displayOrder: 3, status: 'ACTIVE' },
];

const MOCK_STATIONS: TenantStationSummary[] = [
  { id: 1, name: 'Ombrellone 8', type: 'TABLE', areaId: 1, areaName: 'Zona Spiaggia', capacity: 4, status: 'AVAILABLE', active: true, notes: null, activeQrCode: 'demo-qr-01', activeOrdersCount: 0 },
  { id: 2, name: 'Ombrellone 14', type: 'TABLE', areaId: 1, areaName: 'Zona Spiaggia', capacity: 4, status: 'OCCUPIED', active: true, notes: null, activeQrCode: 'demo-qr-02', activeOrdersCount: 1 },
  { id: 3, name: 'Ombrellone 22', type: 'TABLE', areaId: 1, areaName: 'Zona Spiaggia', capacity: 4, status: 'AVAILABLE', active: true, notes: null, activeQrCode: 'demo-qr-03', activeOrdersCount: 0 },
  { id: 4, name: 'Tavolo 3', type: 'TABLE', areaId: 2, areaName: 'Bar Interno', capacity: 6, status: 'OCCUPIED', active: true, notes: null, activeQrCode: 'demo-qr-04', activeOrdersCount: 1 },
  { id: 5, name: 'Terrazzo 1', type: 'TABLE', areaId: 3, areaName: 'Terrazzo', capacity: 8, status: 'OCCUPIED', active: true, notes: null, activeQrCode: 'demo-qr-05', activeOrdersCount: 1 },
];

const MOCK_SETTINGS: TenantSettings = {
  openingTime: '09:00',
  closingTime: '23:00',
  orderingPaused: false,
  ordersViewStartTime: '09:00',
  ordersViewEndTime: '23:00',
};

const FAST_DELAY = 300;

@Injectable()
export class DemoDashboardService extends DashboardService {
  private readonly state = inject(DemoStateService);

  constructor() {
    super(inject(HttpClient));
  }

  override getDashboardMetrics(): Observable<DashboardMetrics> {
    return of(MOCK_METRICS).pipe(delay(FAST_DELAY));
  }

  override refreshMetrics(): Observable<DashboardMetrics> {
    return this.getDashboardMetrics();
  }

  override getTenantProducts(): Observable<TenantProduct[]> {
    return of(MOCK_PRODUCTS).pipe(delay(FAST_DELAY));
  }

  override refreshTenantProducts(): Observable<TenantProduct[]> {
    return this.getTenantProducts();
  }

  override getTenantProductDetails(productId: number): Observable<TenantProductDetails> {
    const p = MOCK_PRODUCTS.find(x => x.id === productId);
    if (!p) return of({ ...MOCK_PRODUCTS[0], variants: [], extras: [] }).pipe(delay(FAST_DELAY));
    return of({ ...p, variants: [], extras: [] }).pipe(delay(FAST_DELAY));
  }

  override createTenantProduct(_p: CreateTenantProductPayload): Observable<TenantProduct> {
    return of(MOCK_PRODUCTS[0]).pipe(delay(FAST_DELAY));
  }

  override updateTenantProduct(_id: number, _p: UpdateTenantProductPayload): Observable<TenantProduct> {
    return of(MOCK_PRODUCTS[0]).pipe(delay(FAST_DELAY));
  }

  override disableTenantProduct(_id: number): Observable<void> {
    return of(undefined).pipe(delay(FAST_DELAY));
  }

  override getTenantOrders(): Observable<TenantOrder[]> {
    return of(this.state.getOrders()).pipe(delay(FAST_DELAY));
  }

  override refreshTenantOrders(): Observable<TenantOrder[]> {
    return of(this.state.getOrders()).pipe(delay(FAST_DELAY));
  }

  override getAllTenantOrders(): Observable<TenantOrder[]> {
    return of(this.state.getOrders()).pipe(delay(FAST_DELAY));
  }

  override refreshAllTenantOrders(): Observable<TenantOrder[]> {
    return of(this.state.getOrders()).pipe(delay(FAST_DELAY));
  }

  override getTenantSettings(): Observable<TenantSettings> {
    return of(MOCK_SETTINGS).pipe(delay(FAST_DELAY));
  }

  override refreshTenantSettings(): Observable<TenantSettings> {
    return this.getTenantSettings();
  }

  override updateTenantSettings(): Observable<TenantSettings> {
    return of(MOCK_SETTINGS).pipe(delay(FAST_DELAY));
  }

  override updateTenantBranding(): Observable<void> {
    return of(undefined).pipe(delay(FAST_DELAY));
  }

  override updateTenantOrderStatus(orderId: number, status: TenantOrderTargetStatus): Observable<void> {
    return of(undefined).pipe(
      delay(FAST_DELAY),
      switchMap(() => {
        this.state.updateOrderStatus(orderId, status);
        return of(undefined);
      })
    );
  }

  override getTenantAreas(): Observable<TenantArea[]> {
    return of(MOCK_AREAS).pipe(delay(FAST_DELAY));
  }

  override refreshTenantAreas(): Observable<TenantArea[]> {
    return this.getTenantAreas();
  }

  override createTenantArea(name: string): Observable<TenantArea> {
    return of({ id: 99, name, displayOrder: 99, status: 'ACTIVE' }).pipe(delay(FAST_DELAY));
  }

  override getTenantAreaById(areaId: number): Observable<TenantArea> {
    return of(MOCK_AREAS.find(a => a.id === areaId) ?? MOCK_AREAS[0]).pipe(delay(FAST_DELAY));
  }

  override updateTenantArea(areaId: number): Observable<TenantArea> {
    return of(MOCK_AREAS.find(a => a.id === areaId) ?? MOCK_AREAS[0]).pipe(delay(FAST_DELAY));
  }

  override disableTenantArea(): Observable<void> {
    return of(undefined).pipe(delay(FAST_DELAY));
  }

  override getTenantStations(): Observable<TenantStationSummary[]> {
    return of(MOCK_STATIONS).pipe(delay(FAST_DELAY));
  }

  override getTenantStationStats(): Observable<TenantStationStats> {
    return of({ total: 5, available: 2, occupied: 3, reserved: 0, orderingDisabled: 0, closed: 0, activeOrders: 3 }).pipe(delay(FAST_DELAY));
  }

  override getTenantStation(id: number): Observable<TenantStationResponse> {
    const s = MOCK_STATIONS.find(x => x.id === id) ?? MOCK_STATIONS[0];
    return of({
      id: s.id, tenantId: 999, name: s.name, type: s.type,
      areaId: s.areaId, areaName: s.areaName, capacity: s.capacity,
      status: s.status, active: s.active, notes: s.notes,
      qrCode: s.activeQrCode, qrUrl: null, qrActive: true,
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString()
    }).pipe(delay(FAST_DELAY));
  }

  override createTenantStation(): Observable<TenantStationResponse> {
    return this.getTenantStation(1);
  }

  override updateTenantStation(id: number): Observable<TenantStationResponse> {
    return this.getTenantStation(id);
  }

  override deleteTenantStation(): Observable<void> {
    return of(undefined).pipe(delay(FAST_DELAY));
  }

  override getTenantStationQr(stationId: number): Observable<TenantStationQr> {
    return of({
      id: stationId, stationId, code: `demo-qr-0${stationId}`,
      qrValue: 'https://demo.orderapp.it', qrImageBase64: '',
      active: true, generatedAt: new Date().toISOString(), regeneratedAt: null, downloadUrl: null
    }).pipe(delay(FAST_DELAY));
  }

  override generateTenantStationQr(stationId: number): Observable<TenantStationQr> {
    return this.getTenantStationQr(stationId);
  }

  override regenerateTenantStationQr(stationId: number): Observable<TenantStationQr> {
    return this.getTenantStationQr(stationId);
  }

  override downloadTenantStationQr(): Observable<Blob> {
    return of(new Blob(['demo'], { type: 'image/png' })).pipe(delay(FAST_DELAY));
  }

  override downloadAllTenantStationsQr(): Observable<Blob> {
    return of(new Blob(['demo'], { type: 'application/zip' })).pipe(delay(FAST_DELAY));
  }

  override resetCache(): void {}
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry, shareReplay, tap, timeout } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface DashboardMetrics {
  totalRevenueToday: number;
  totalOrdersToday: number;
  activeCustomerCount: number;
  averageOrderTime: number;
  lastUpdated: string;
  ordersByHour: OrderByHour[];
  weeklyRevenue: WeeklyRevenue[];
  topProducts: TopProduct[];
  areaDistribution: AreaDistribution[];
}

export interface OrderByHour {
  hour: number;
  count: number;
}

export interface WeeklyRevenue {
  day: string;
  revenue: number;
}

export interface TopProduct {
  id: number;
  name: string;
  quantity: number;
}

export interface AreaDistribution {
  areaId: number;
  areaName: string;
  orderCount: number;
}

export interface TenantArea {
  id: number;
  name: string;
  displayOrder: number;
  status: string;
}

export interface TenantStationSummary {
  id: number;
  name: string;
  type: string;
  areaId: number | null;
  areaName: string | null;
  capacity: number | null;
  status: string;
  active: boolean;
  notes: string | null;
  activeQrCode: string | null;
  activeOrdersCount: number;
}

export interface TenantStationStats {
  total: number;
  available: number;
  occupied: number;
  reserved: number;
  orderingDisabled: number;
  closed: number;
  activeOrders: number;
}

export interface TenantStationResponse {
  id: number;
  tenantId: number;
  name: string;
  type: string;
  areaId: number | null;
  areaName: string | null;
  capacity: number | null;
  status: string;
  active: boolean;
  notes: string | null;
  qrCode: string | null;
  qrUrl: string | null;
  qrActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TenantStationQr {
  id: number;
  stationId: number;
  code: string;
  qrValue: string;
  qrImageBase64: string;
  active: boolean;
  generatedAt: string;
  regeneratedAt: string | null;
  downloadUrl: string | null;
}

export interface TenantProduct {
  id: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  category: string;
  department: 'BAR' | 'KITCHEN' | 'SERVICE' | 'GENERIC';
  vatRate: number;
  status: string;
  availableForOrder: boolean;
  variantsCount: number;
  extrasCount: number;
  sku: string;
}

export interface TenantProductOption {
  name: string;
  priceDelta: number;
}

export interface TenantProductDetails extends TenantProduct {
  variants: TenantProductOption[];
  extras: TenantProductOption[];
}

export interface TenantOrderLine {
  quantity: number;
  name: string;
  total: number;
}

export interface TenantOrder {
  id: number;
  code: string;
  locationLabel: string;
  areaName: string;
  type: 'BAR' | 'KITCHEN';
  status: string;
  note: string | null;
  total: number;
  createdAt: string;
  items: TenantOrderLine[];
}

export type TenantOrderTargetStatus = 'DELIVERED' | 'CANCELLED';

export interface TenantSettings {
  openingTime: string;
  closingTime: string;
  orderingPaused: boolean;
  ordersViewStartTime: string;
  ordersViewEndTime: string;
}

export interface UpdateTenantSettingsPayload {
  openingTime?: string;
  closingTime?: string;
  orderingPaused?: boolean;
  ordersViewStartTime?: string;
  ordersViewEndTime?: string;
}

export interface CreateTenantProductPayload {
  name: string;
  description: string;
  price: number;
  category: string;
  availableForOrder: boolean;
  imageDataUrl: string | null;
  variants: TenantProductOption[];
  extras: TenantProductOption[];
}

export interface UpdateTenantProductPayload {
  name: string;
  description: string;
  price: number;
  category: string;
  availableForOrder: boolean;
  imageDataUrl: string | null;
  variants?: TenantProductOption[];
  extras?: TenantProductOption[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly API_URL = `${environment.apiUrl}/api/dashboard`;
  private metricsRequest$: Observable<DashboardMetrics> | null = null;

  constructor(private http: HttpClient) {}

  getDashboardMetrics(forceRefresh = false): Observable<DashboardMetrics> {
    if (forceRefresh || !this.metricsRequest$) {
      console.info('[dashboard.service] Fetching metrics', {
        forceRefresh,
        url: `${this.API_URL}/metrics`,
        at: new Date().toISOString()
      });

      this.metricsRequest$ = this.http.get<DashboardMetrics>(`${this.API_URL}/metrics`).pipe(
        timeout(12000),
        retry({ count: 1, delay: 300 }),
        tap((metrics) => {
          console.info('[dashboard.service] Metrics response received', {
            totalOrdersToday: metrics.totalOrdersToday,
            topProducts: metrics.topProducts?.length ?? 0,
            areaDistribution: metrics.areaDistribution?.length ?? 0,
            at: new Date().toISOString()
          });
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
        catchError((error) => {
          console.error('[dashboard.service] Metrics request failed', {
            status: error?.status,
            message: error?.message,
            url: error?.url,
            error: error?.error
          });
          this.metricsRequest$ = null;
          return throwError(() => error);
        })
      );
    }
    return this.metricsRequest$;
  }

  refreshMetrics(): Observable<DashboardMetrics> {
    return this.getDashboardMetrics(true);
  }

  getTenantProducts(): Observable<TenantProduct[]> {
    console.info('[dashboard.service] Fetching tenant products', { url: `${this.API_URL}/products`, at: new Date().toISOString() });
    return this.http.get<TenantProduct[]>(`${this.API_URL}/products`);
  }

  getTenantProductDetails(productId: number): Observable<TenantProductDetails> {
    return this.http.get<TenantProductDetails>(`${this.API_URL}/products/${productId}`);
  }

  refreshTenantProducts(): Observable<TenantProduct[]> {
    console.info('[dashboard.service] Refreshing tenant products', { url: `${this.API_URL}/products`, at: new Date().toISOString() });
    return this.http.get<TenantProduct[]>(`${this.API_URL}/products`);
  }

  createTenantProduct(payload: CreateTenantProductPayload): Observable<TenantProduct> {
    return this.http.post<TenantProduct>(`${this.API_URL}/products`, payload);
  }

  updateTenantProduct(productId: number, payload: UpdateTenantProductPayload): Observable<TenantProduct> {
    return this.http.patch<TenantProduct>(`${this.API_URL}/products/${productId}`, payload);
  }

  disableTenantProduct(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/products/${productId}`);
  }

  getTenantOrders(): Observable<TenantOrder[]> {
    console.info('[dashboard.service] Fetching tenant orders', { url: `${this.API_URL}/orders`, at: new Date().toISOString() });
    return this.http.get<TenantOrder[]>(`${this.API_URL}/orders`);
  }

  refreshTenantOrders(): Observable<TenantOrder[]> {
    console.info('[dashboard.service] Refreshing tenant orders', { url: `${this.API_URL}/orders`, at: new Date().toISOString() });
    return this.http.get<TenantOrder[]>(`${this.API_URL}/orders`);
  }

  getAllTenantOrders(limit = 300): Observable<TenantOrder[]> {
    console.info('[dashboard.service] Fetching all tenant orders', {
      url: `${this.API_URL}/orders/all`,
      limit,
      at: new Date().toISOString()
    });
    return this.http.get<TenantOrder[]>(`${this.API_URL}/orders/all`, { params: this.toHttpParams({ limit }) });
  }

  refreshAllTenantOrders(limit = 300): Observable<TenantOrder[]> {
    console.info('[dashboard.service] Refreshing all tenant orders', {
      url: `${this.API_URL}/orders/all`,
      limit,
      at: new Date().toISOString()
    });
    return this.http.get<TenantOrder[]>(`${this.API_URL}/orders/all`, { params: this.toHttpParams({ limit }) });
  }

  getTenantSettings(): Observable<TenantSettings> {
    console.info('[dashboard.service] Fetching tenant settings', { url: `${this.API_URL}/settings`, at: new Date().toISOString() });
    return this.http.get<TenantSettings>(`${this.API_URL}/settings`);
  }

  refreshTenantSettings(): Observable<TenantSettings> {
    console.info('[dashboard.service] Refreshing tenant settings', { url: `${this.API_URL}/settings`, at: new Date().toISOString() });
    return this.http.get<TenantSettings>(`${this.API_URL}/settings`);
  }

  updateTenantSettings(payload: UpdateTenantSettingsPayload): Observable<TenantSettings> {
    console.info('[dashboard.service] Updating tenant settings', { url: `${this.API_URL}/settings`, at: new Date().toISOString() });
    return this.http.patch<TenantSettings>(`${this.API_URL}/settings`, payload);
  }

  updateTenantBranding(logoDataUrl: string | null): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/branding`, { logoDataUrl });
  }

  updateTenantOrderStatus(orderId: number, status: TenantOrderTargetStatus): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/orders/${orderId}/status`, { status });
  }

  getTenantAreas(): Observable<TenantArea[]> {
    return this.http.get<TenantArea[]>(`${this.API_URL}/areas`);
  }

  refreshTenantAreas(): Observable<TenantArea[]> {
    return this.http.get<TenantArea[]>(`${this.API_URL}/areas`);
  }

  createTenantArea(name: string): Observable<TenantArea> {
    return this.http.post<TenantArea>(`${this.API_URL}/areas`, { name });
  }

  getTenantAreaById(areaId: number): Observable<TenantArea> {
    return this.http.get<TenantArea>(`${this.API_URL}/areas/${areaId}`);
  }

  updateTenantArea(areaId: number, payload: { name?: string; displayOrder?: number; status?: 'ACTIVE' | 'DISABLED' }): Observable<TenantArea> {
    return this.http.patch<TenantArea>(`${this.API_URL}/areas/${areaId}`, payload);
  }

  disableTenantArea(areaId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/areas/${areaId}`);
  }

  getTenantStations(params?: { name?: string; areaId?: number; type?: string; status?: string; active?: boolean }): Observable<TenantStationSummary[]> {
    return this.http.get<TenantStationSummary[]>(`${environment.apiUrl}/api/staff/stations`, { params: this.toHttpParams(params) });
  }

  getTenantStationStats(): Observable<TenantStationStats> {
    return this.http.get<TenantStationStats>(`${environment.apiUrl}/api/staff/stations/stats`);
  }

  getTenantStation(stationId: number): Observable<TenantStationResponse> {
    return this.http.get<TenantStationResponse>(`${environment.apiUrl}/api/staff/stations/${stationId}`);
  }

  createTenantStation(payload: {
    name: string;
    type: string;
    areaId: number;
    capacity: number;
    status: string;
    notes: string;
    active: boolean;
    generateQrAutomatically: boolean;
  }): Observable<TenantStationResponse> {
    return this.http.post<TenantStationResponse>(`${environment.apiUrl}/api/staff/stations`, payload);
  }

  updateTenantStation(stationId: number, payload: Partial<{
    name: string;
    type: string;
    areaId: number;
    capacity: number;
    status: string;
    notes: string;
    active: boolean;
  }>): Observable<TenantStationResponse> {
    return this.http.put<TenantStationResponse>(`${environment.apiUrl}/api/staff/stations/${stationId}`, payload);
  }

  deleteTenantStation(stationId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/staff/stations/${stationId}`);
  }

  getTenantStationQr(stationId: number): Observable<TenantStationQr> {
    return this.http.get<TenantStationQr>(`${environment.apiUrl}/api/staff/stations/${stationId}/qr`);
  }

  generateTenantStationQr(stationId: number, regenerate = false): Observable<TenantStationQr> {
    return this.http.post<TenantStationQr>(`${environment.apiUrl}/api/staff/stations/${stationId}/qr`, null, {
      params: regenerate ? { regenerate: 'true' } : undefined
    });
  }

  regenerateTenantStationQr(stationId: number): Observable<TenantStationQr> {
    return this.http.post<TenantStationQr>(`${environment.apiUrl}/api/staff/stations/${stationId}/qr/regenerate`, null);
  }

  downloadTenantStationQr(stationId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/staff/stations/${stationId}/qr/download`, { responseType: 'blob' });
  }

  downloadAllTenantStationsQr(): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/staff/stations/qr/download-all`, { responseType: 'blob' });
  }

  resetCache(): void {
    this.metricsRequest$ = null;
  }

  private toHttpParams(params?: Record<string, string | number | boolean | undefined>): HttpParams {
    let httpParams = new HttpParams();
    if (!params) {
      return httpParams;
    }

    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return httpParams;
  }
}

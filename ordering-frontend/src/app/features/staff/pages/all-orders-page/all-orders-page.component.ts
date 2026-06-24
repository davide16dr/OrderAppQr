import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ChangeDetectorRef } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Subject, timer } from 'rxjs';
import { auditTime, filter, finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { DashboardService, TenantArea, TenantOrder } from '../../services/dashboard.service';
import { OrderEventsWsService } from '../../services/order-events-ws.service';
import { OrderNotificationService } from '../../../../core/services/order-notification.service';

@Component({
  selector: 'app-all-orders-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './all-orders-page.component.html',
  styleUrl: './all-orders-page.component.scss'
})
export class AllOrdersPageComponent implements OnInit, OnDestroy {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dashboardService = inject(DashboardService);
  private readonly orderEventsWs = inject(OrderEventsWsService);
  private readonly orderNotification = inject(OrderNotificationService);
  private dayFilterAuto = true;
  private readonly destroy$ = new Subject<void>();
  private initialLoadWatchdogId: ReturnType<typeof setTimeout> | null = null;
  private pendingLoads = 0;

  orders: TenantOrder[] = [];
  tenantAreas: TenantArea[] = [];
  filtered: TenantOrder[] = [];
  grouped: Array<{ dayKey: string; label: string; count: number; total: number; orders: TenantOrder[] }> = [];

  searchTerm = '';
  statusFilter = 'ALL';
  dayFilter = 'ALL';
  areaFilter = 'ALL';
  locationFilter = 'ALL';

  ordersViewStartTime = '00:00';
  ordersViewEndTime = '23:59';

  isLoading = false;
  hasError = false;
  errorMessage = 'Errore durante il caricamento ordini.';

  ngOnInit(): void {
    this.loadTenantAreas();
    this.loadOrders(true);
    this.loadSettings();

    this.orderEventsWs.ensureConnected();

    this.orderEventsWs.events$
      .pipe(auditTime(700), takeUntil(this.destroy$))
      .subscribe(() => this.loadOrders(false));

    this.orderEventsWs.events$
      .pipe(filter(e => e.eventType === 'ORDER_CREATED'), takeUntil(this.destroy$))
      .subscribe(() => this.orderNotification.playNewOrder());

    timer(10000, 10000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.isLoading) {
          this.loadOrders(false);
        }
      });
  }

  ngOnDestroy(): void {
    if (this.initialLoadWatchdogId) {
      clearTimeout(this.initialLoadWatchdogId);
      this.initialLoadWatchdogId = null;
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTenantAreas(): void {
    this.dashboardService
      .getTenantAreas()
      .pipe(
        timeout(8000),
        retry({ count: 1, delay: 200 }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (areas) => {
          this.tenantAreas = Array.isArray(areas) ? areas : [];
          if (this.areaFilter !== 'ALL' && !this.areaOptions.some((opt) => opt.value === this.areaFilter)) {
            this.areaFilter = 'ALL';
          }
          this.applyFilters();
          this.cdr.markForCheck();
        },
        error: () => {
          this.tenantAreas = [];
          this.cdr.markForCheck();
        }
      });
  }

  private loadSettings(): void {
    this.dashboardService
      .refreshTenantSettings()
      .pipe(
        timeout(8000),
        retry({ count: 1, delay: 200 }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (settings) => {
          this.ordersViewStartTime = settings.ordersViewStartTime || '00:00';
          this.ordersViewEndTime = settings.ordersViewEndTime || '23:59';
          if (this.dayFilterAuto) {
            this.dayFilter = this.toBusinessDayKey(new Date());
          }
          this.applyFilters();
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        }
      });
  }

  loadOrders(scheduleWatchdog = false): void {
    this.isLoading = true;
    this.pendingLoads += 1;
    this.hasError = false;

    if (scheduleWatchdog) {
      if (this.initialLoadWatchdogId) {
        clearTimeout(this.initialLoadWatchdogId);
      }
      this.initialLoadWatchdogId = setTimeout(() => {
        // First-open fallback: if the first request is still hanging, trigger one extra refresh.
        if (this.isLoading) {
          this.loadOrders(false);
        }
      }, 3500);
    }

    this.dashboardService
      .refreshAllTenantOrders(500)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        takeUntil(this.destroy$),
        finalize(() => {
          this.pendingLoads = Math.max(0, this.pendingLoads - 1);
          this.isLoading = this.pendingLoads > 0;
          if (this.initialLoadWatchdogId) {
            clearTimeout(this.initialLoadWatchdogId);
            this.initialLoadWatchdogId = null;
          }
        })
      )
      .subscribe({
        next: (orders) => {
          this.orders = Array.isArray(orders) ? orders : [];
          this.ensureCurrentBusinessDayFilter();
          this.applyFilters();
          this.cdr.markForCheck();
        },
        error: () => {
          this.orders = [];
          this.filtered = [];
          this.grouped = [];
          this.hasError = true;
          this.cdr.markForCheck();
        }
      });
  }

  applyFilters(): void {
    const query = this.searchTerm.trim().toLowerCase();
    const hasCustomWindow = this.hasCustomWindowConfigured();

    this.filtered = this.orders.filter((order) => {
      const matchesQuery = !query
        || order.code.toLowerCase().includes(query)
        || order.locationLabel.toLowerCase().includes(query)
        || (order.areaName || '').toLowerCase().includes(query);

      const orderStatusGroup = this.getStatusFilterValue(order.status);
      const matchesStatus = this.statusFilter === 'ALL' || orderStatusGroup === this.statusFilter;
      const matchesArea = this.areaFilter === 'ALL' || order.areaName === this.areaFilter;
      const matchesLocation = this.locationFilter === 'ALL' || order.locationLabel === this.locationFilter;

      const createdAt = new Date(order.createdAt);
      const dayKey = this.toBusinessDayKey(createdAt);
      const matchesDay = this.dayFilter === 'ALL' || dayKey === this.dayFilter;

      // Time-window filter: show only orders created within the configured window.
      // (No current-time check here — all-orders is a historical view.)
      const matchesWindow = !hasCustomWindow || this.isWithinOrdersViewWindow(createdAt);

      return matchesQuery && matchesStatus && matchesArea && matchesLocation && matchesDay && matchesWindow;
    });

    this.grouped = this.groupOrdersByDay(this.filtered);
  }

  /** True when the tenant has set a window other than the full-day default (00:00–23:59). */
  private hasCustomWindowConfigured(): boolean {
    const start = this.parseMinutes(this.ordersViewStartTime) ?? 0;
    const end   = this.parseMinutes(this.ordersViewEndTime)   ?? 23 * 60 + 59;
    return !(start === 0 && end >= 23 * 60 + 59);
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
    this.applyFilters();
  }

  onStatusChange(value: string): void {
    this.statusFilter = value;
    this.applyFilters();
  }

  onDayChange(value: string): void {
    this.dayFilter = value;
    this.dayFilterAuto = value === 'ALL';

    if (this.areaFilter !== 'ALL' && !this.areaOptions.some((opt) => opt.value === this.areaFilter)) {
      this.areaFilter = 'ALL';
    }

    if (this.locationFilter !== 'ALL' && !this.locationOptions.some((opt) => opt.value === this.locationFilter)) {
      this.locationFilter = 'ALL';
    }

    this.applyFilters();
  }

  onAreaChange(value: string): void {
    this.areaFilter = value;

    if (this.locationFilter !== 'ALL' && !this.locationOptions.some((opt) => opt.value === this.locationFilter)) {
      this.locationFilter = 'ALL';
    }

    this.applyFilters();
  }

  onLocationChange(value: string): void {
    this.locationFilter = value;
    this.applyFilters();
  }

  get dayOptions(): Array<{ value: string; label: string; count: number }> {
    const hasCustomWindow = this.hasCustomWindowConfigured();
    const counts = new Map<string, number>();
    for (const order of this.orders) {
      const createdAt = new Date(order.createdAt);
      // Only count orders within the time window so the displayed count is consistent
      // with what applyFilters actually shows.
      if (hasCustomWindow && !this.isWithinOrdersViewWindow(createdAt)) {
        continue;
      }
      const dayKey = this.toBusinessDayKey(createdAt);
      counts.set(dayKey, (counts.get(dayKey) ?? 0) + 1);
    }

    const dayKeys = Array.from(counts.keys()).sort((a, b) => b.localeCompare(a));
    const visibleCount = Array.from(counts.values()).reduce((sum, value) => sum + value, 0);

    return [
      { value: 'ALL', label: 'Tutti i giorni', count: visibleCount },
      ...dayKeys.map((dayKey) => ({
        value: dayKey,
        label: this.labelForDayKey(dayKey),
        count: counts.get(dayKey) ?? 0
      }))
    ];
  }

  get areaOptions(): Array<{ value: string; label: string; count: number }> {
    const hasCustomWindow = this.hasCustomWindowConfigured();
    const candidateOrders = this.orders.filter((order) => {
      const createdAt = new Date(order.createdAt);
      const dayKey = this.toBusinessDayKey(createdAt);
      const matchesDay = this.dayFilter === 'ALL' || dayKey === this.dayFilter;
      const matchesStatus = this.statusFilter === 'ALL' || order.status === this.statusFilter;
      const inWindow = !hasCustomWindow || this.isWithinOrdersViewWindow(createdAt);
      return matchesDay && matchesStatus && inWindow;
    });

    const counts = new Map<string, number>();
    for (const order of candidateOrders) {
      counts.set(order.areaName, (counts.get(order.areaName) ?? 0) + 1);
    }

    const tenantAreaNames = this.tenantAreas
      .filter((a) => a.status === 'ACTIVE')
      .slice()
      .sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name))
      .map((a) => a.name);

    const dynamicAreaNames = Array.from(counts.keys())
      .filter((name) => !tenantAreaNames.includes(name))
      .sort((a, b) => (counts.get(b) ?? 0) - (counts.get(a) ?? 0) || a.localeCompare(b));

    const allAreaNames = [...tenantAreaNames, ...dynamicAreaNames];

    return [
      { value: 'ALL', label: 'Tutte le aree', count: candidateOrders.length },
      ...allAreaNames.map((area) => ({
        value: area,
        label: area,
        count: counts.get(area) ?? 0
      }))
    ];
  }

  get locationOptions(): Array<{ value: string; label: string; count: number }> {
    const hasCustomWindow = this.hasCustomWindowConfigured();
    const candidateOrders = this.orders.filter((order) => {
      const createdAt = new Date(order.createdAt);
      const dayKey = this.toBusinessDayKey(createdAt);
      const matchesDay = this.dayFilter === 'ALL' || dayKey === this.dayFilter;
      const matchesStatus = this.statusFilter === 'ALL' || order.status === this.statusFilter;
      const matchesArea = this.areaFilter === 'ALL' || order.areaName === this.areaFilter;
      const inWindow = !hasCustomWindow || this.isWithinOrdersViewWindow(createdAt);
      return matchesDay && matchesStatus && matchesArea && inWindow;
    });

    const counts = new Map<string, number>();
    for (const order of candidateOrders) {
      counts.set(order.locationLabel, (counts.get(order.locationLabel) ?? 0) + 1);
    }

    const locationNames = Array.from(counts.keys())
      .sort((a, b) => (counts.get(b) ?? 0) - (counts.get(a) ?? 0) || a.localeCompare(b));

    return [
      { value: 'ALL', label: 'Tutte le postazioni', count: candidateOrders.length },
      ...locationNames.map((location) => ({
        value: location,
        label: location,
        count: counts.get(location) ?? 0
      }))
    ];
  }

  private ensureCurrentBusinessDayFilter(): void {
    if (this.dayFilter !== 'ALL') {
      return;
    }

    this.dayFilterAuto = true;
    this.dayFilter = this.toBusinessDayKey(new Date());
  }

  private parseMinutes(value: string): number | null {
    const trimmed = (value || '').trim();
    if (trimmed.length !== 5 || trimmed.charAt(2) !== ':') {
      return null;
    }

    const hh = Number(trimmed.slice(0, 2));
    const mm = Number(trimmed.slice(3, 5));
    if (!Number.isFinite(hh) || !Number.isFinite(mm) || hh < 0 || hh > 23 || mm < 0 || mm > 59) {
      return null;
    }

    return hh * 60 + mm;
  }

  private isWithinOrdersViewWindow(date: Date): boolean {
    const start = this.parseMinutes(this.ordersViewStartTime) ?? 0;
    const end = this.parseMinutes(this.ordersViewEndTime) ?? 23 * 60 + 59;

    const minutes = date.getHours() * 60 + date.getMinutes();

    const wraps = end < start;
    if (!wraps) {
      return minutes >= start && minutes <= end;
    }

    return minutes >= start || minutes <= end;
  }

  private toBusinessDayKey(date: Date): string {
    const start = this.parseMinutes(this.ordersViewStartTime) ?? 0;
    const end = this.parseMinutes(this.ordersViewEndTime) ?? 23 * 60 + 59;

    const minutes = date.getHours() * 60 + date.getMinutes();
    const wraps = end < start;

    const adjusted = new Date(date);
    if (wraps && minutes <= end) {
      adjusted.setDate(adjusted.getDate() - 1);
    }

    return this.toLocalDayKey(adjusted);
  }

  private toLocalDayKey(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private dateFromDayKey(dayKey: string): Date {
    const [y, m, d] = dayKey.split('-').map((part) => Number(part));
    return new Date(y, (m ?? 1) - 1, d ?? 1);
  }

  private labelForDayKey(dayKey: string): string {
    const now = new Date();
    const todayKey = this.toBusinessDayKey(now);
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayKey = this.toBusinessDayKey(yesterday);

    if (dayKey === todayKey) {
      return 'Oggi';
    }

    if (dayKey === yesterdayKey) {
      return 'Ieri';
    }

    const date = this.dateFromDayKey(dayKey);
    const sameYear = date.getFullYear() === new Date().getFullYear();
    return date.toLocaleDateString('it-IT', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      ...(sameYear ? {} : { year: 'numeric' })
    });
  }

  private groupOrdersByDay(orders: TenantOrder[]): Array<{ dayKey: string; label: string; count: number; total: number; orders: TenantOrder[] }> {
    const map = new Map<string, TenantOrder[]>();

    for (const order of orders) {
      const dayKey = this.toBusinessDayKey(new Date(order.createdAt));
      const bucket = map.get(dayKey) ?? [];
      bucket.push(order);
      map.set(dayKey, bucket);
    }

    const dayKeys = Array.from(map.keys()).sort((a, b) => b.localeCompare(a));

    return dayKeys.map((dayKey) => {
      const dayOrders = (map.get(dayKey) ?? []).slice().sort((a, b) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });

      return {
        dayKey,
        label: this.labelForDayKey(dayKey),
        count: dayOrders.length,
        total: dayOrders.reduce((sum, o) => sum + o.total, 0),
        orders: dayOrders
      };
    });
  }

  statusTone(status: string): 'neutral' | 'success' | 'warning' | 'danger' {
    if (status === 'DELIVERED') {
      return 'success';
    }
    if (status === 'CANCELLED') {
      return 'danger';
    }
    return 'warning';
  }

  private getStatusFilterValue(status: string): 'RECEIVED' | 'DELIVERED' | 'CANCELLED' {
    if (status === 'DELIVERED') {
      return 'DELIVERED';
    }

    if (status === 'CANCELLED') {
      return 'CANCELLED';
    }

    return 'RECEIVED';
  }

  trackGroup(_: number, group: { dayKey: string }): string {
    return group.dayKey;
  }

  trackOrder(_: number, order: TenantOrder): number {
    return order.id;
  }

  statusLabel(status: string): string {
    return status === 'DELIVERED' ? 'Consegnato' : 'Ricevuto';
  }
}

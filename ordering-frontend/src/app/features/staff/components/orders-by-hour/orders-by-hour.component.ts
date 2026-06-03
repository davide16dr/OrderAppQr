import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, timer } from 'rxjs';
import { auditTime, catchError, finalize, map, retry, takeUntil, timeout } from 'rxjs/operators';
import { of } from 'rxjs';
import { DashboardService, TenantArea, TenantOrder } from '../../services/dashboard.service';
import { OrderStatus, StaffOrderCard } from '../../models/staff-order.model';
import { OrderDetailsOverlayComponent } from './order-details-overlay/order-details-overlay.component';
import { OrderEventsWsService } from '../../services/order-events-ws.service';

type OrderCardVM = StaffOrderCard & {
  createdAtMs: number;
  dayKey: string;
};

type DayGroup<T> = {
  dayKey: string;
  label: string;
  orders: T[];
  count: number;
  total: number;
};

@Component({
  selector: 'app-orders-by-hour',
  standalone: true,
  imports: [CommonModule, OrderDetailsOverlayComponent],
  templateUrl: './orders-by-hour.component.html',
  styleUrls: ['./orders-by-hour.component.scss']
})
export class OrdersByHourComponent implements OnInit, OnDestroy {
  orders: OrderCardVM[] = [];
  tenantAreas: TenantArea[] = [];
  isLoading = true;
  hasError = false;
  selectedDay = 'ALL';
  selectedArea = 'ALL';
  selectedLocation = 'ALL';
  selectedOrderId: number | null = null;

  ordersViewStartTime = '00:00';
  ordersViewEndTime = '23:59';
  private readonly updatingOrderIds = new Set<number>();
  private destroy$ = new Subject<void>();
  private readonly orderEventsWs = inject(OrderEventsWsService);

  constructor(
    private dashboardService: DashboardService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSettings();
    this.loadTenantAreas();
    this.loadData(true);


    this.orderEventsWs.ensureConnected();
    this.orderEventsWs.events$
      .pipe(auditTime(500), takeUntil(this.destroy$))
      .subscribe(() => this.loadData(true));

    timer(10000, 10000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.isLoading) {
          this.loadData(true);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reloadOrders(): void {
    this.loadData(true);
  }

  private loadData(forceRefresh = false): void {
    this.isLoading = true;
    this.hasError = false;
    this.changeDetectorRef.markForCheck();
    console.info('[orders-by-hour] loadData start', { forceRefresh });
    const orders$ = forceRefresh
      ? this.dashboardService.refreshTenantOrders()
      : this.dashboardService.getTenantOrders();

    orders$
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        map((orders) => {
          const normalizedOrders = Array.isArray(orders) ? orders : [];
          console.info('[orders-by-hour] orders payload received', {
            count: normalizedOrders.length,
            isArray: Array.isArray(orders)
          });
          return normalizedOrders.map((order) => this.mapOrder(order));
        }),
        catchError((err) => {
          console.error('[orders-by-hour] loadData error', {
            status: err?.status,
            message: err?.message,
            error: err?.error
          });
          this.orders = [];
          this.hasError = true;
          this.changeDetectorRef.markForCheck();
          return of([] as OrderCardVM[]);
        }),
        finalize(() => {
          this.isLoading = false;
          this.changeDetectorRef.markForCheck();
          console.info('[orders-by-hour] loadData finalized', {
            hasError: this.hasError,
            visibleOrders: this.orders.length
          });
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (orders) => {
          this.orders = orders;
          this.ensureCurrentBusinessDaySelected();
          this.hasError = false;
          this.changeDetectorRef.markForCheck();
        },
        error: () => undefined
      });
  }

  get dayOptions(): Array<{ value: string; label: string; count: number }> {
    const counts = new Map<string, number>();
    for (const order of this.orders) {
      // Count only orders that fall within the configured time window so that
      // the displayed count always matches what is actually visible on the board.
      if (this.hasCustomWindow && !this.isWithinOrdersViewWindow(order.createdAtMs)) {
        continue;
      }
      counts.set(order.dayKey, (counts.get(order.dayKey) ?? 0) + 1);
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
    const candidateOrders = this.orders.filter((order) => {
      const dayMatch = this.selectedDay === 'ALL' || order.dayKey === this.selectedDay;
      const inWindow = !this.hasCustomWindow || this.isWithinOrdersViewWindow(order.createdAtMs);
      return dayMatch && inWindow;
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
    const candidateOrders = this.orders.filter((order) => {
      const dayMatch = this.selectedDay === 'ALL' || order.dayKey === this.selectedDay;
      const areaMatch = this.selectedArea === 'ALL' || order.areaName === this.selectedArea;
      const inWindow = !this.hasCustomWindow || this.isWithinOrdersViewWindow(order.createdAtMs);
      return dayMatch && areaMatch && inWindow;
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

  get filteredOrders(): OrderCardVM[] {
    const todayKey = this.computeBusinessDayKey(new Date());

    return this.orders.filter((order) => {
      const dayMatch = this.selectedDay === 'ALL' || order.dayKey === this.selectedDay;
      const areaMatch = this.selectedArea === 'ALL' || order.areaName === this.selectedArea;
      const locationMatch = this.selectedLocation === 'ALL' || order.locationLabel === this.selectedLocation;

      // Time-window filter (only meaningful when a custom window is configured):
      // 1. Order must have been created within the [start, end] window.
      // 2. For today's orders the window must still be currently active; for past
      //    days the historical orders are always visible regardless of clock time.
      const orderInWindow = !this.hasCustomWindow || this.isWithinOrdersViewWindow(order.createdAtMs);
      const isToday = order.dayKey === todayKey;
      const windowActive = !this.hasCustomWindow || !isToday || this.isWindowCurrentlyActive;

      return dayMatch && areaMatch && locationMatch && orderInWindow && windowActive;
    });
  }

  /** True when the wall-clock time currently falls within the configured view window. */
  get isWindowCurrentlyActive(): boolean {
    const start = this.parseMinutes(this.ordersViewStartTime) ?? 0;
    const end   = this.parseMinutes(this.ordersViewEndTime)   ?? 23 * 60 + 59;
    const now   = new Date();
    const cur   = now.getHours() * 60 + now.getMinutes();
    const wraps = end < start;
    return wraps ? (cur >= start || cur <= end) : (cur >= start && cur <= end);
  }

  /** True when the window is something other than the full-day default (00:00-23:59). */
  get hasCustomWindow(): boolean {
    const start = this.parseMinutes(this.ordersViewStartTime) ?? 0;
    const end   = this.parseMinutes(this.ordersViewEndTime)   ?? 23 * 60 + 59;
    return !(start === 0 && end >= 23 * 60 + 59);
  }

  /** Human-readable window label, e.g. "10:00 – 14:00". */
  get windowLabel(): string {
    return `${this.ordersViewStartTime} – ${this.ordersViewEndTime}`;
  }

  get receivedOrders(): OrderCardVM[] {
    return this.filteredOrders.filter((order) => order.status === 'received');
  }

  get deliveredOrders(): OrderCardVM[] {
    return this.filteredOrders.filter((order) => order.status === 'delivered');
  }

  get receivedGroups(): Array<DayGroup<OrderCardVM>> {
    return this.groupOrdersByDay(this.receivedOrders);
  }

  get deliveredGroups(): Array<DayGroup<OrderCardVM>> {
    return this.groupOrdersByDay(this.deliveredOrders);
  }

  get totalValueVisible(): number {
    return this.filteredOrders.reduce((sum, order) => sum + order.total, 0);
  }

  get selectedOrder(): OrderCardVM | null {
    if (this.selectedOrderId === null) {
      return null;
    }

    return this.orders.find((order) => order.id === this.selectedOrderId) ?? null;
  }

  getStatusLabel(status: OrderStatus): string {
    if (status === 'received') {
      return 'Ricevuto';
    }

    return 'Consegnato';
  }

  getNextStatusLabel(status: OrderStatus): string {
    if (status === 'received') {
      return 'Segna consegnato';
    }

    return 'Nessuna azione';
  }

  canAdvanceStatus(status: OrderStatus): boolean {
    return status !== 'delivered';
  }

  advanceOrderStatus(orderId: number): void {
    const targetOrder = this.orders.find((order) => order.id === orderId);
    if (!targetOrder || targetOrder.status === 'delivered' || this.updatingOrderIds.has(orderId)) {
      return;
    }

    this.updatingOrderIds.add(orderId);

    this.dashboardService.updateTenantOrderStatus(orderId, this.getNextBackendStatus(targetOrder.status))
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.updatingOrderIds.delete(orderId);
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.orders = this.orders.map((order) => {
            if (order.id !== orderId) {
              return order;
            }

            return { ...order, status: 'delivered' };
          });
          this.hasError = false;
          this.changeDetectorRef.markForCheck();
        },
        error: (err) => {
          console.error('Error updating order status:', err);
          this.hasError = true;
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  openOrderDetails(orderId: number): void {
    this.selectedOrderId = orderId;
  }

  closeOrderDetails(): void {
    this.selectedOrderId = null;
  }

  onOverlayAdvanceStatus(orderId: number): void {
    this.closeOrderDetails();
    this.advanceOrderStatus(orderId);
  }

  onDayChange(day: string): void {
    this.selectedDay = day;

    // Se l'area selezionata non esiste più nel giorno selezionato, reset.
    if (this.selectedArea !== 'ALL' && !this.areaOptions.some((opt) => opt.value === this.selectedArea)) {
      this.selectedArea = 'ALL';
    }
  }

  onAreaChange(area: string): void {
    this.selectedArea = area;
    if (this.selectedLocation !== 'ALL' && !this.locationOptions.some((opt) => opt.value === this.selectedLocation)) {
      this.selectedLocation = 'ALL';
    }
  }

  onLocationChange(location: string): void {
    this.selectedLocation = location;
  }

  trackOrder(index: number, order: StaffOrderCard): number {
    return order.id;
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
          this.recomputeOrderDayKeys();
          this.ensureCurrentBusinessDaySelected();
          this.changeDetectorRef.markForCheck();
        },
        error: () => undefined
      });
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
          this.changeDetectorRef.markForCheck();
        },
        error: () => {
          this.tenantAreas = [];
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  private recomputeOrderDayKeys(): void {
    if (!this.orders.length) {
      return;
    }

    this.orders = this.orders.map((order) => ({
      ...order,
      dayKey: this.computeBusinessDayKey(new Date(order.createdAtMs))
    }));
  }

  private ensureCurrentBusinessDaySelected(): void {
    const currentBusinessDay = this.computeBusinessDayKey(new Date());

    if (this.selectedDay === 'ALL') {
      this.selectedDay = currentBusinessDay;
    }

    if (this.selectedArea !== 'ALL' && !this.areaOptions.some((opt) => opt.value === this.selectedArea)) {
      this.selectedArea = 'ALL';
    }
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

  private isWithinOrdersViewWindow(createdAtMs: number): boolean {
    const start = this.parseMinutes(this.ordersViewStartTime) ?? 0;
    const end = this.parseMinutes(this.ordersViewEndTime) ?? 23 * 60 + 59;

    const date = new Date(createdAtMs);
    const minutes = date.getHours() * 60 + date.getMinutes();

    const wraps = end < start;
    if (!wraps) {
      return minutes >= start && minutes <= end;
    }

    return minutes >= start || minutes <= end;
  }

  private computeBusinessDayKey(date: Date): string {
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
    const todayKey = this.computeBusinessDayKey(now);
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayKey = this.computeBusinessDayKey(yesterday);

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

  private groupOrdersByDay(orders: OrderCardVM[]): Array<DayGroup<OrderCardVM>> {
    const map = new Map<string, OrderCardVM[]>();

    for (const order of orders) {
      const bucket = map.get(order.dayKey) ?? [];
      bucket.push(order);
      map.set(order.dayKey, bucket);
    }

    const dayKeys = Array.from(map.keys()).sort((a, b) => b.localeCompare(a));

    return dayKeys.map((dayKey) => {
      const dayOrders = (map.get(dayKey) ?? []).slice().sort((a, b) => b.createdAtMs - a.createdAtMs);
      return {
        dayKey,
        label: this.labelForDayKey(dayKey),
        orders: dayOrders,
        count: dayOrders.length,
        total: dayOrders.reduce((sum, o) => sum + o.total, 0)
      };
    });
  }

  private mapOrder(order: TenantOrder): OrderCardVM {
    const createdAt = new Date(order.createdAt);
    const nowMs = Date.now();
    const createdMs = createdAt.getTime();
    const minutesAgo = Math.max(0, Math.floor((nowMs - createdMs) / 60000));

    return {
      id: order.id,
      code: order.code,
      locationLabel: order.locationLabel,
      areaName: order.areaName,
      type: order.type,
      minutesAgo,
      timeLabel: createdAt.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
      dateLabel: createdAt.toLocaleDateString('it-IT', { day: '2-digit', month: 'long', year: 'numeric' }),
      status: this.mapStatus(order.status),
      items: (order.items || []).map((item) => {
        const name = item.name;
        const reportedVariant = (item as any).variant || (item as any).variantName || undefined;
        const fallbackFromName = reportedVariant ? undefined : this.extractSuffixFromName(name);
        const rawVariant = reportedVariant || fallbackFromName;
        return {
          quantity: item.quantity,
          name,
          total: item.total,
          variant: rawVariant || undefined,
          extras: this.extractModifierDetails(rawVariant).extras
        };
      }),
      total: order.total,
      note: order.note || undefined,
      createdAtMs: createdMs,
      dayKey: this.computeBusinessDayKey(createdAt)
    };
  }

  formatInlineDetails(line: { variant?: string; extras?: string[] } & { name?: string }): string {
    const details = this.extractModifierDetails(line.variant);
    const parts = [...details.variants, ...details.extras];
    return parts.join(', ');
  }

  private extractModifierDetails(raw?: string): { variants: string[]; extras: string[] } {
    if (!raw || !raw.trim()) {
      return { variants: [], extras: [] };
    }

    const tokens = raw
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean);

    const variants: string[] = [];
    const extras: string[] = [];
    let activeBucket: 'variants' | 'extras' | null = null;

    for (const token of tokens) {
      const lower = token.toLowerCase();
      if (lower.startsWith('varianti:')) {
        activeBucket = 'variants';
        const value = token.slice(token.indexOf(':') + 1).trim();
        if (value) {
          variants.push(value);
        }
        continue;
      }

      if (lower.startsWith('extra:')) {
        activeBucket = 'extras';
        const value = token.slice(token.indexOf(':') + 1).trim();
        if (value) {
          extras.push(value);
        }
        continue;
      }

      if (activeBucket === 'extras') {
        extras.push(token);
      } else {
        variants.push(token);
      }
    }

    return { variants, extras };
  }

  private extractSuffixFromName(name?: string): string | undefined {
    if (!name) return undefined;
    const m = name.match(/\(([^)]+)\)\s*$/);
    if (!m) return undefined;
    const inside = m[1]?.trim();
    return inside && inside.length ? inside : undefined;
  }

  private mapStatus(status: string): OrderStatus {
    if (status === 'DELIVERED') {
      return 'delivered';
    }

    return 'received';
  }

  private getNextBackendStatus(status: OrderStatus): 'DELIVERED' {
    if (status === 'received') {
      return 'DELIVERED';
    }

    return 'DELIVERED';
  }
}

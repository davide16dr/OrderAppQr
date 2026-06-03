import {
  ChangeDetectorRef, Component, Input,
  OnInit, OnDestroy, OnChanges, SimpleChanges
} from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { DashboardService, DashboardMetrics } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard-main',
  standalone: true,
  imports: [DecimalPipe, DatePipe],
  templateUrl: './dashboard-main.component.html',
  styleUrls: ['./dashboard-main.component.scss']
})
export class DashboardMainComponent implements OnInit, OnDestroy, OnChanges {
  @Input() refreshTrigger = 0;

  metrics: DashboardMetrics | null = null;
  isLoading = true;
  hasError = false;

  private destroy$ = new Subject<void>();

  constructor(
    private dashboardService: DashboardService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void { this.loadData(); }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['refreshTrigger'] && !changes['refreshTrigger'].firstChange) {
      this.loadData();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Dynamic max for orders-by-hour bars */
  get ordersByHourMax(): number {
    if (!this.metrics?.ordersByHour?.length) return 1;
    return Math.max(...this.metrics.ordersByHour.map(d => d.count), 1);
  }

  /** Dynamic max for weekly-revenue bars */
  get weeklyRevenueMax(): number {
    if (!this.metrics?.weeklyRevenue?.length) return 1;
    return Math.max(...this.metrics.weeklyRevenue.map(d => d.revenue), 1);
  }

  /** Total orders across all areas (for area distribution percentages) */
  get totalAreaOrders(): number {
    if (!this.metrics?.areaDistribution?.length) return 1;
    return Math.max(
      this.metrics.areaDistribution.reduce((s, a) => s + a.orderCount, 0),
      1
    );
  }

  getOrderBarHeight(count: number): number {
    return (count / this.ordersByHourMax) * 100;
  }

  getRevenueBarHeight(revenue: number): number {
    return (revenue / this.weeklyRevenueMax) * 100;
  }

  getAreaPct(count: number): number {
    return (count / this.totalAreaOrders) * 100;
  }

  formatHour(hour: number): string {
    return String(hour).padStart(2, '0') + ':00';
  }

  loadData(): void {
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    this.dashboardService.refreshMetrics()
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (metrics) => {
          this.metrics = metrics;
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.metrics = null;
          this.cdr.markForCheck();
        }
      });
  }
}

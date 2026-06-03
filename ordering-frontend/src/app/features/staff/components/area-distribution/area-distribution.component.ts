import { ChangeDetectorRef, Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-area-distribution',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './area-distribution.component.html',
  styleUrls: ['./area-distribution.component.scss']
})
export class AreaDistributionComponent implements OnInit, OnDestroy, OnChanges {
  @Input() refreshTrigger = 0;

  areaDistribution: any[] = [];
  isLoading = true;
  hasError = false;
  maxOrders = 0;
  totalOrders = 0;

  private readonly COLORS = [
    '#2f6de0', '#16a34a', '#d97706', '#9333ea',
    '#db2777', '#0891b2', '#dc2626', '#65a30d'
  ];

  private destroy$ = new Subject<void>();
  private loadingGuardId: ReturnType<typeof setTimeout> | null = null;
  private loadSeq = 0;

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
    if (this.loadingGuardId) { clearTimeout(this.loadingGuardId); }
    this.destroy$.next();
    this.destroy$.complete();
  }

  getPercentage(orders: number): number {
    return this.totalOrders > 0 ? (orders / this.totalOrders) * 100 : 0;
  }

  getColor(index: number): string {
    return this.COLORS[index % this.COLORS.length];
  }

  /** Computes a conic-gradient string that reflects actual data proportions. */
  get donutGradient(): string {
    if (!this.areaDistribution.length) return '#e5e7eb';
    let deg = 0;
    const stops = this.areaDistribution.map((area: any, i: number) => {
      const degrees = (area.orderCount / this.totalOrders) * 360;
      const stop = `${this.getColor(i)} ${deg}deg ${deg + degrees}deg`;
      deg += degrees;
      return stop;
    });
    return `conic-gradient(from 0deg, ${stops.join(', ')})`;
  }

  private loadData(): void {
    this.loadSeq += 1;
    const seq = this.loadSeq;
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    if (this.loadingGuardId) { clearTimeout(this.loadingGuardId); }
    this.loadingGuardId = setTimeout(() => {
      if (this.isLoading && this.loadSeq === seq) {
        this.isLoading = false;
        this.hasError = true;
        this.cdr.markForCheck();
      }
    }, 15000);

    this.dashboardService.refreshMetrics()
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoading = false;
          if (this.loadingGuardId) { clearTimeout(this.loadingGuardId); this.loadingGuardId = null; }
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (metrics) => {
          this.areaDistribution = metrics.areaDistribution || [];
          this.totalOrders = this.areaDistribution.reduce((sum: number, a: any) => sum + a.orderCount, 0);
          this.maxOrders = Math.max(...this.areaDistribution.map((a: any) => a.orderCount), 1);
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.areaDistribution = [];
          this.cdr.markForCheck();
        }
      });
  }
}

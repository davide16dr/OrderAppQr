import { ChangeDetectorRef, Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-weekly-revenue',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './weekly-revenue.component.html',
  styleUrls: ['./weekly-revenue.component.scss']
})
export class WeeklyRevenueComponent implements OnInit, OnDestroy, OnChanges {
  @Input() refreshTrigger = 0;

  weeklyRevenue: any[] = [];
  isLoading = true;
  hasError = false;
  maxRevenue = 0;
  totalRevenue = 0;

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

  getBarHeight(revenue: number): number {
    return (revenue / this.maxRevenue) * 100;
  }

  formatBarLabel(revenue: number): string {
    if (revenue >= 1000) return `€${(revenue / 1000).toFixed(1)}k`;
    return `€${revenue.toFixed(0)}`;
  }

  get dailyAverage(): number {
    return this.weeklyRevenue.length ? this.totalRevenue / this.weeklyRevenue.length : 0;
  }

  get bestDayRevenue(): number {
    return this.maxRevenue;
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
          this.weeklyRevenue = metrics.weeklyRevenue || [];
          this.maxRevenue = Math.max(...this.weeklyRevenue.map((w: any) => w.revenue), 1);
          this.totalRevenue = this.weeklyRevenue.reduce((sum: number, w: any) => sum + w.revenue, 0);
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.weeklyRevenue = [];
          this.cdr.markForCheck();
        }
      });
  }
}

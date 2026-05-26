import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-area-distribution',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './area-distribution.component.html',
  styleUrls: ['./area-distribution.component.scss']
})
export class AreaDistributionComponent implements OnInit, OnDestroy {
  areaDistribution: any[] = [];
  isLoading = true;
  hasError = false;
  maxOrders = 0;
  totalOrders = 0;
  private destroy$ = new Subject<void>();
  private loadingGuardId: ReturnType<typeof setTimeout> | null = null;
  private loadSeq = 0;

  constructor(
    private dashboardService: DashboardService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    if (this.loadingGuardId) {
      clearTimeout(this.loadingGuardId);
      this.loadingGuardId = null;
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadData(): void {
    this.loadSeq += 1;
    const currentSeq = this.loadSeq;
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    if (this.loadingGuardId) {
      clearTimeout(this.loadingGuardId);
    }
    this.loadingGuardId = setTimeout(() => {
      if (this.isLoading && this.loadSeq === currentSeq) {
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
          if (this.loadingGuardId) {
            clearTimeout(this.loadingGuardId);
            this.loadingGuardId = null;
          }
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (metrics) => {
          this.areaDistribution = metrics.areaDistribution || [];
          this.maxOrders = Math.max(...this.areaDistribution.map(a => a.orderCount), 1);
          this.totalOrders = this.areaDistribution.reduce((sum, a) => sum + a.orderCount, 0);
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error loading area distribution:', err);
          this.hasError = true;
          this.areaDistribution = [];
          this.cdr.markForCheck();
        }
      });
  }

  getPercentage(orders: number): number {
    return (orders / this.totalOrders) * 100;
  }

  getColor(index: number): string {
    const colors = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe', '#ec4899', '#f59e0b'];
    return colors[index % colors.length];
  }
}

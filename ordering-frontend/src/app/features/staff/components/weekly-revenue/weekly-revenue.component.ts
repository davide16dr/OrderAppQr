import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-weekly-revenue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './weekly-revenue.component.html',
  styleUrls: ['./weekly-revenue.component.scss']
})
export class WeeklyRevenueComponent implements OnInit, OnDestroy {
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
          this.weeklyRevenue = metrics.weeklyRevenue || [];
          this.maxRevenue = Math.max(...this.weeklyRevenue.map(w => w.revenue), 1);
          this.totalRevenue = this.weeklyRevenue.reduce((sum, w) => sum + w.revenue, 0);
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error loading weekly revenue:', err);
          this.hasError = true;
          this.weeklyRevenue = [];
          this.cdr.markForCheck();
        }
      });
  }

  getPercentage(revenue: number): number {
    return (revenue / this.maxRevenue) * 100;
  }
}

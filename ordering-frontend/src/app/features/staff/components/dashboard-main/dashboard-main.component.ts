import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DashboardService, DashboardMetrics } from '../../services/dashboard.service';
import { LoadingStateComponent } from '../shared/loading-state/loading-state.component';
import { ErrorStateComponent } from '../shared/error-state/error-state.component';
import { EmptyStateComponent } from '../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-dashboard-main',
  standalone: true,
  imports: [CommonModule, LoadingStateComponent, ErrorStateComponent, EmptyStateComponent],
  templateUrl: './dashboard-main.component.html',
  styleUrls: ['./dashboard-main.component.scss']
})
export class DashboardMainComponent implements OnInit, OnDestroy {
  metrics: DashboardMetrics | null = null;
  isLoading = true;
  hasError = false;
  private destroy$ = new Subject<void>();
  private loadStartMs = 0;

  constructor(
    private dashboardService: DashboardService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.info('[dashboard-main] ngOnInit, starting initial load');
    this.loadMetrics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadMetrics(): void {
    this.loadStartMs = Date.now();
    console.info('[dashboard-main] loadMetrics start');
    this.isLoading = true;
    this.hasError = false;
    this.dashboardService.refreshMetrics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (metrics) => {
          this.metrics = metrics;
          this.isLoading = false;
          this.cdr.markForCheck();
          console.info('[dashboard-main] loadMetrics success', {
            elapsedMs: Date.now() - this.loadStartMs,
            totalOrdersToday: metrics.totalOrdersToday,
            totalRevenueToday: metrics.totalRevenueToday
          });
        },
        error: (err) => {
          console.error('Error loading dashboard metrics:', err);
          this.hasError = true;
          this.metrics = null;
          this.isLoading = false;
          this.cdr.markForCheck();
          console.error('[dashboard-main] loadMetrics error', {
            elapsedMs: Date.now() - this.loadStartMs,
            status: err?.status,
            message: err?.message,
            error: err?.error
          });
        }
      });
  }

  refreshMetrics(): void {
    this.loadStartMs = Date.now();
    console.info('[dashboard-main] refreshMetrics start');
    this.isLoading = true;
    this.hasError = false;
    this.dashboardService.refreshMetrics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (metrics) => {
          this.metrics = metrics;
          this.isLoading = false;
          this.cdr.markForCheck();
          console.info('[dashboard-main] refreshMetrics success', {
            elapsedMs: Date.now() - this.loadStartMs,
            totalOrdersToday: metrics.totalOrdersToday
          });
        },
        error: (err) => {
          console.error('Error refreshing metrics:', err);
          this.hasError = true;
          this.metrics = null;
          this.isLoading = false;
          this.cdr.markForCheck();
          console.error('[dashboard-main] refreshMetrics error', {
            elapsedMs: Date.now() - this.loadStartMs,
            status: err?.status,
            message: err?.message,
            error: err?.error
          });
        }
      });
  }

  getTrendClass(change: number): string {
    if (change > 0) return 'trend-up';
    if (change < 0) return 'trend-down';
    return 'trend-stable';
  }

  getTrendIcon(change: number): string {
    if (change > 0) return '↑';
    if (change < 0) return '↓';
    return '→';
  }

  formatHour(hour: number): string {
    return String(hour).padStart(2, '0') + ':00';
  }
}

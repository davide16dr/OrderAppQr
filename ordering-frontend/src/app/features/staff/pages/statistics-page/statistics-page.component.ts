import { Component } from '@angular/core';
import { WeeklyRevenueComponent } from '../../components/weekly-revenue/weekly-revenue.component';
import { AreaDistributionComponent } from '../../components/area-distribution/area-distribution.component';

@Component({
  selector: 'app-statistics-page',
  standalone: true,
  imports: [WeeklyRevenueComponent, AreaDistributionComponent],
  template: `
    <div class="st-page">
      <div class="st-header">
        <div class="st-header-text">
          <h1 class="st-title">Statistiche</h1>
          <p class="st-sub">Panoramica delle performance dell'attività</p>
        </div>
        <button type="button" class="st-refresh" (click)="refresh()">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          Aggiorna
        </button>
      </div>
      <div class="st-grid">
        <app-weekly-revenue [refreshTrigger]="refreshCount"></app-weekly-revenue>
        <app-area-distribution [refreshTrigger]="refreshCount"></app-area-distribution>
      </div>
    </div>
  `,
  styles: [`
    .st-page { display: flex; flex-direction: column; gap: 16px; }
    .st-header {
      display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap;
    }
    .st-title { margin: 0; font-size: 22px; font-weight: 800; color: #1f2a37; letter-spacing: -0.02em; }
    .st-sub { margin: 2px 0 0; font-size: 13px; color: #697485; }
    .st-refresh {
      display: inline-flex; align-items: center; gap: 6px;
      border: 1px solid #dde4ef; border-radius: 10px; background: #fff;
      color: #1f2a37; font-weight: 700; font-size: 13px; padding: 8px 14px; cursor: pointer;
    }
    .st-refresh:hover { background: #f4f7fb; }
    .st-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 16px;
    }
  `]
})
export class StatisticsPageComponent {
  refreshCount = 0;
  refresh(): void { this.refreshCount++; }
}

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WeeklyRevenueComponent } from '../../components/weekly-revenue/weekly-revenue.component';
import { AreaDistributionComponent } from '../../components/area-distribution/area-distribution.component';

@Component({
  selector: 'app-statistics-page',
  standalone: true,
  imports: [CommonModule, WeeklyRevenueComponent, AreaDistributionComponent],
  template: `
    <section class="stats-grid">
      <app-weekly-revenue></app-weekly-revenue>
      <app-area-distribution></app-area-distribution>
    </section>
  `,
  styles: ['.stats-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:16px;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatisticsPageComponent {}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-chart-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="chart-card">
      <header *ngIf="title"><h3>{{ title }}</h3></header>
      <ng-content></ng-content>
    </section>
  `,
  styles: ['.chart-card{background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:14px;} h3{margin:0 0 10px;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChartCardComponent {
  @Input() title = '';
}

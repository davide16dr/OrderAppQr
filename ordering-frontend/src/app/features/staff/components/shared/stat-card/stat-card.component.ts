import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="card">
      <p class="label">{{ label }}</p>
      <p class="value">{{ value }}</p>
    </article>
  `,
  styles: [`.card{background:#fff;border-radius:10px;padding:14px;border:1px solid #e5e7eb}.label{margin:0;color:#6b7280;font-size:12px}.value{margin:6px 0 0;font-size:24px;font-weight:700}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatCardComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) value!: string | number;
}

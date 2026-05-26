import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantOrder } from '../../../services/dashboard.service';

@Component({
  selector: 'app-order-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="order-card" *ngIf="order">
      <p class="code">{{ order.code }}</p>
      <p>{{ order.locationLabel }} - {{ order.areaName }}</p>
      <p><strong>€{{ order.total | number: '1.2-2' }}</strong></p>
      <p class="status">{{ order.status }}</p>
    </article>
  `,
  styles: ['.order-card{background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:10px;} .code{font-weight:700;margin:0 0 4px;} .status{font-size:12px;color:#6b7280;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderCardComponent {
  @Input() order: TenantOrder | null = null;
}

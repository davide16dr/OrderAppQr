import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantProduct } from '../../../services/dashboard.service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="product-card" *ngIf="product">
      <h4>{{ product.name }}</h4>
      <p>{{ product.category }}</p>
      <p><strong>€{{ product.price | number: '1.2-2' }}</strong></p>
    </article>
  `,
  styles: ['.product-card{background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:10px;} h4{margin:0 0 6px;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductCardComponent {
  @Input() product: TenantProduct | null = null;
}

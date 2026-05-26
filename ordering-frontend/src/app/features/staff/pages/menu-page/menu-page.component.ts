import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TopProductsComponent } from '../../components/top-products/top-products.component';

@Component({
  selector: 'app-menu-page',
  standalone: true,
  imports: [CommonModule, TopProductsComponent],
  template: `<app-top-products></app-top-products>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuPageComponent {}

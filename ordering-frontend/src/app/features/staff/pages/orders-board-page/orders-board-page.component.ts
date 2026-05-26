import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrdersByHourComponent } from '../../components/orders-by-hour/orders-by-hour.component';

@Component({
  selector: 'app-orders-board-page',
  standalone: true,
  imports: [CommonModule, OrdersByHourComponent],
  template: `<app-orders-by-hour></app-orders-by-hour>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrdersBoardPageComponent {}

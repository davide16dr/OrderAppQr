import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { formatEuroFromCents } from '../../models/customer.types';

@Component({
  selector: 'app-cart-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cart-bar.component.html',
  styleUrl: './cart-bar.component.scss',
})
export class CartBarComponent {
  @Input({ required: true }) totalQuantity = 0;
  @Input({ required: true }) totalCents = 0;

  @Output() openCart = new EventEmitter<void>();

  euro(cents: number): string {
    return formatEuroFromCents(cents);
  }
}

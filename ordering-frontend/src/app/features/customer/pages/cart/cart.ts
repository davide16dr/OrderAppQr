import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CartSheetComponent } from '../../components/cart-sheet/cart-sheet.component';
import { CartService } from '../../services/cart';
import { CustomerOrder } from '../../services/customer-order';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, CartSheetComponent],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class Cart {
  private readonly router = inject(Router);
  readonly cart = inject(CartService);
  private readonly customerOrder = inject(CustomerOrder);

  close(): void {
    const context = this.customerOrder.getOrderContext();
    this.router.navigate(['/customer/menu'], { queryParams: this.buildMenuQueryParams(context) });
  }

  private buildMenuQueryParams(context: { token: string | null; tenant: string | null; location: string | null }): Record<string, string> {
    return {
      ...(context.token ? { token: context.token } : {}),
      ...(context.tenant ? { tenant: context.tenant } : {}),
      ...(context.location ? { location: context.location } : {}),
    };
  }

  remove(productId: string): void {
    this.cart.removeProduct(productId);
  }

  dec(productId: string): void {
    this.cart.decrementProduct(productId);
  }

  inc(productId: string): void {
    this.cart.incrementLine(productId);
  }

  submit(): void {
    this.customerOrder.placeOrder(this.cart.lines(), this.cart.note()).subscribe(() => {
      this.cart.clear();
      this.router.navigate(['/customer/order']);
    });
  }
}

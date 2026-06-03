import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomerMenuViewModel, MenuProduct, ModifierGroup } from '../../models/customer.types';
import { CartService } from '../../services/cart';
import { CustomerMenu } from '../../services/customer-menu';
import { CustomerOrder } from '../../services/customer-order';
import { CartBarComponent } from '../../components/cart-bar/cart-bar.component';
import { CartSheetComponent } from '../../components/cart-sheet/cart-sheet.component';
import { CategoryTabsComponent } from '../../components/category-tabs/category-tabs.component';
import { LocationHeaderComponent } from '../../components/location-header/location-header.component';
import { ProductCardComponent } from '../../components/product-card/product-card.component';

@Component({
  selector: 'app-location-menu',
  standalone: true,
  imports: [
    CurrencyPipe,
    LocationHeaderComponent,
    CategoryTabsComponent,
    ProductCardComponent,
    CartBarComponent,
    CartSheetComponent,
  ],
  templateUrl: './location-menu.html',
  styleUrl: './location-menu.scss',
})
export class LocationMenu implements OnInit {
  private static readonly ORDER_CONTEXT_KEY = 'orderapp.customer.orderContext';

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly menuService = inject(CustomerMenu);
  readonly cart = inject(CartService);
  private readonly customerOrder = inject(CustomerOrder);

  readonly vm = signal<CustomerMenuViewModel | null>(null);
  readonly selectedCategoryId = signal<string | null>(null);
  readonly isCartOpen = signal(false);
  readonly selectedProduct = signal<MenuProduct | null>(null);
  readonly selectedModifierOptionIds = signal<number[]>([]);
  readonly selectionError = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const token = params.get('token') ?? params.get('station');
      const tenant = params.get('tenant');
      const location = params.get('location');

      this.persistOrderContext(token, tenant, location);

      this.menuService.getMenu({ token, tenant, location }).subscribe(menuVm => {
        this.vm.set(menuVm);
        this.selectedCategoryId.set(menuVm.categories[0]?.id ?? null);

        if (!this.isOrderingAvailable()) {
          this.cart.clear();
          this.closeCart();
        }
      });
    });
  }

  private persistOrderContext(token: string | null, tenant: string | null, location: string | null): void {
    try {
      sessionStorage.setItem(LocationMenu.ORDER_CONTEXT_KEY, JSON.stringify({ token, tenant, location }));
    } catch {
      // Ignore storage issues.
    }
  }

  get filteredProducts(): MenuProduct[] {
    const vm = this.vm();
    if (!vm) return [];
    return vm.products.filter(p => p.categoryId === this.selectedCategoryId());
  }

  openCart(): void { this.isCartOpen.set(true); }
  closeCart(): void { this.isCartOpen.set(false); }

  add(product: MenuProduct): void {
    if (this.hasModifierGroups(product)) {
      this.openProductPicker(product);
      return;
    }
    this.cart.addProduct(product);
  }

  increment(product: MenuProduct): void { this.add(product); }

  decrement(productIdOrLineKey: string): void {
    const line = this.cart.lines().find(l => l.lineKey === productIdOrLineKey || l.productId === productIdOrLineKey);
    if (line) this.cart.decrementProduct(line.lineKey);
  }

  remove(lineKey: string): void { this.cart.removeProduct(lineKey); }

  openProductPicker(product: MenuProduct): void {
    this.selectedProduct.set(product);
    this.selectedModifierOptionIds.set([]);
    this.selectionError.set(null);
  }

  closeProductPicker(): void {
    this.selectedProduct.set(null);
    this.selectedModifierOptionIds.set([]);
    this.selectionError.set(null);
  }

  hasModifierGroups(product: MenuProduct): boolean {
    return (product.modifierGroups ?? []).some(g => (g.options?.length ?? 0) > 0);
  }

  pickerOptionControlType(group: ModifierGroup): 'radio' | 'checkbox' {
    return group.maxSelectable === 1 ? 'radio' : 'checkbox';
  }

  pickerIsOptionSelected(optionId: number): boolean {
    return this.selectedModifierOptionIds().includes(optionId);
  }

  pickerToggleOption(group: ModifierGroup, optionId: number): void {
    const product = this.selectedProduct();
    if (!product) return;

    const current = new Set<number>(this.selectedModifierOptionIds().filter(n => Number.isFinite(n)));
    const isSingle = group.maxSelectable === 1;

    if (isSingle) {
      for (const opt of group.options ?? []) current.delete(opt.id);
      current.add(optionId);
    } else {
      if (current.has(optionId)) {
        current.delete(optionId);
      } else {
        const maxSelectable = group.maxSelectable ?? null;
        if (typeof maxSelectable === 'number' && maxSelectable > 0) {
          const selectedInGroup = (group.options ?? []).filter(o => current.has(o.id)).length;
          if (selectedInGroup >= maxSelectable) return;
        }
        current.add(optionId);
      }
    }

    this.selectedModifierOptionIds.set(Array.from(current.values()));
    this.selectionError.set(null);
  }

  confirmSelectedProduct(): void {
    const product = this.selectedProduct();
    if (!product) return;

    const selectedIds = this.selectedModifierOptionIds();
    const validationError = this.validateSelection(product, selectedIds);
    if (validationError) {
      this.selectionError.set(validationError);
      return;
    }

    this.cart.addProduct(product, selectedIds);
    this.closeProductPicker();
  }

  private validateSelection(product: MenuProduct, selectedIds: number[]): string | null {
    for (const group of product.modifierGroups ?? []) {
      const selectedInGroup = (group.options ?? []).filter(o => selectedIds.includes(o.id)).length;
      if (group.maxSelectable === 1 && selectedInGroup > 1) {
        return `Puoi selezionare una sola opzione per ${group.name}.`;
      }
    }
    return null;
  }

  submitOrder(): void {
    if (!this.isOrderingAvailable()) return;

    const lines = this.cart.lines();
    const note = this.cart.note();
    this.customerOrder.placeOrder(lines, note).subscribe({
      next: () => {
        this.cart.clear();
        this.closeCart();
        this.router.navigate(['/customer/order']);
      },
      error: () => {
        alert('Impossibile inviare l\'ordine. Riprova tra poco.');
      }
    });
  }

  isOrderingAvailable(): boolean {
    return this.vm()?.context.statusVariant === 'active';
  }
}

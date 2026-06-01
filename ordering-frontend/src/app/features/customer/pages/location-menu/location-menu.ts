import { CommonModule } from '@angular/common';
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
    CommonModule,
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
        console.info('Customer menu context loaded', {
          businessName: menuVm.context.businessName,
          logoPresent: !!menuVm.context.businessLogoDataUrl,
        });
        console.log('📦 MENU LOADED:', {
          productsCount: menuVm.products.length,
          productsWithModifiers: menuVm.products.filter(p => (p.modifierGroups?.length ?? 0) > 0).length
        });
        menuVm.products.forEach(p => {
          if (p.modifierGroups?.length) {
            console.log(`   ✅ ${p.name}: ${p.modifierGroups.length} modifier groups`);
            p.modifierGroups.forEach(g => {
              console.log(`      - ${g.name}: ${g.options?.length ?? 0} options`);
            });
          }
        });
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
    const context = {
      token: token ?? null,
      tenant: tenant ?? null,
      location: location ?? null
    };

    try {
      sessionStorage.setItem(LocationMenu.ORDER_CONTEXT_KEY, JSON.stringify(context));
    } catch {
      // Ignore storage issues to keep the page functional.
    }
  }

  get filteredProducts(): MenuProduct[] {
    const vm = this.vm();
    if (!vm) return [];
    const categoryId = this.selectedCategoryId();
    return vm.products.filter(p => p.categoryId === categoryId);
  }

  trackByProductId(_: number, product: MenuProduct): string {
    return product.id;
  }

  openCart(): void {
    this.isCartOpen.set(true);
  }

  closeCart(): void {
    this.isCartOpen.set(false);
  }

  add(product: MenuProduct): void {
    console.log(`🔵 ADD clicked: ${product.name}`, {
      hasModifiers: this.hasModifierGroups(product),
      modifierCount: product.modifierGroups?.length ?? 0
    });

    if (this.hasModifierGroups(product)) {
      console.log('📋 Opening picker...');
      this.openProductPicker(product);
      return;
    }

    console.log('➕ Adding directly to cart (no modifiers)');
    this.cart.addProduct(product);
  }

  increment(product: MenuProduct): void {
    this.add(product);
  }

  decrement(lineKey: string): void {
    this.cart.decrementProduct(lineKey);
  }

  remove(lineKey: string): void {
    this.cart.removeProduct(lineKey);
  }

  openProductPicker(product: MenuProduct): void {
    console.error('🟡 PICKER OPEN for:', product.id);
    this.selectedProduct.set(product);
    console.error('   selectedProduct set, signal value:', this.selectedProduct());
    this.selectedModifierOptionIds.set([]);
    console.error('   selectedModifierOptionIds set:', this.selectedModifierOptionIds());
    this.selectionError.set(null);
  }

  closeProductPicker(): void {
    console.error('🔴 PICKER CLOSED');
    this.selectedProduct.set(null);
    this.selectedModifierOptionIds.set([]);
    this.selectionError.set(null);
  }

  hasModifierGroups(product: MenuProduct): boolean {
    return (product.modifierGroups?.length ?? 0) > 0;
  }

  pickerOptionControlType(group: ModifierGroup): 'radio' | 'checkbox' {
    return group.maxSelectable === 1 ? 'radio' : 'checkbox';
  }

  pickerIsOptionSelected(optionId: number): boolean {
    return this.selectedModifierOptionIds().includes(optionId);
  }

  pickerToggleOption(group: ModifierGroup, optionId: number): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }

    const current = new Set<number>(this.selectedModifierOptionIds().filter(n => Number.isFinite(n)));
    const isSingle = group.maxSelectable === 1;

    if (isSingle) {
      for (const opt of group.options ?? []) {
        current.delete(opt.id);
      }
      current.add(optionId);
    } else {
      if (current.has(optionId)) {
        current.delete(optionId);
      } else {
        const maxSelectable = group.maxSelectable ?? null;
        if (typeof maxSelectable === 'number' && maxSelectable > 0) {
          const selectedInGroup = (group.options ?? []).filter(option => current.has(option.id)).length;
          if (selectedInGroup >= maxSelectable) {
            return;
          }
        }
        current.add(optionId);
      }
    }

    this.selectedModifierOptionIds.set(Array.from(current.values()));
    this.selectionError.set(null);
  }

  confirmSelectedProduct(): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }

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
      const selectedInGroup = (group.options ?? []).filter(option => selectedIds.includes(option.id)).length;

      if (group.maxSelectable === 1 && selectedInGroup > 1) {
        return `Puoi selezionare una sola opzione per ${group.name}.`;
      }
    }

    return null;
  }

  submitOrder(): void {
    if (!this.isOrderingAvailable()) {
      return;
    }

    const lines = this.cart.lines();
    const note = this.cart.note();
    this.customerOrder.placeOrder(lines, note).subscribe({
      next: () => {
        this.cart.clear();
        this.closeCart();
        this.router.navigate(['/customer/order']);
      },
      error: (err) => {
        console.error('Failed to create order', err);
        alert('Impossibile inviare l\'ordine. Riprova tra poco.');
      }
    });
  }

  isOrderingAvailable(): boolean {
    return this.vm()?.context.statusVariant === 'active';
  }
}

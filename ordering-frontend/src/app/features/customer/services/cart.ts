import { Injectable, computed, signal } from '@angular/core';
import { CartLine, MenuProduct } from '../models/customer.types';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly linesSignal = signal<CartLine[]>([]);
  readonly note = signal('');

  readonly lines = computed(() => this.linesSignal());
  readonly totalQuantity = computed(() =>
    this.linesSignal().reduce((sum, line) => sum + line.quantity, 0)
  );
  readonly totalCents = computed(() =>
    this.linesSignal().reduce((sum, line) => sum + line.unitPriceCents * line.quantity, 0)
  );

  quantityForProduct(productId: string): number {
    return this.linesSignal().find(l => l.productId === productId)?.quantity ?? 0;
  }

  getSelectionForProduct(productId: string): number[] {
    return this.linesSignal().find(l => l.productId === productId)?.selectedModifierOptionIds ?? [];
  }

  setNote(note: string): void {
    this.note.set(note);
  }

  addProduct(product: MenuProduct, selectedModifierOptionIds: number[] = []): void {
    const normalizedSelectedIds = Array.from(new Set((selectedModifierOptionIds ?? []).filter(n => Number.isFinite(n))));
    const unitPriceCents = this.computeUnitPriceCents(product.priceCents, product.modifierGroups, normalizedSelectedIds);

    const existing = this.linesSignal().find(l => l.productId === product.id);
    if (!existing) {
      this.linesSignal.update(lines => [
        ...lines,
        {
          productId: product.id,
          name: product.name,
          description: product.description,
          baseUnitPriceCents: product.priceCents,
          unitPriceCents,
          imageUrl: product.imageUrl,
          icon: product.icon,
          quantity: 1,
          modifierGroups: product.modifierGroups ?? [],
          selectedModifierOptionIds: normalizedSelectedIds,
        },
      ]);
      return;
    }

    this.linesSignal.update(lines =>
      lines.map(l => (
        l.productId === product.id
          ? {
              ...l,
              quantity: l.quantity + 1,
              modifierGroups: product.modifierGroups ?? l.modifierGroups,
              selectedModifierOptionIds: normalizedSelectedIds,
              baseUnitPriceCents: product.priceCents,
              unitPriceCents,
            }
          : l
      ))
    );
  }

  incrementProduct(product: MenuProduct): void {
    const existing = this.linesSignal().find(l => l.productId === product.id);
    if (!existing) {
      this.linesSignal.update(lines => [
        ...lines,
        {
          productId: product.id,
          name: product.name,
          description: product.description,
          baseUnitPriceCents: product.priceCents,
          unitPriceCents: product.priceCents,
          imageUrl: product.imageUrl,
          icon: product.icon,
          quantity: 1,
          modifierGroups: product.modifierGroups ?? [],
          selectedModifierOptionIds: [],
        },
      ]);
      return;
    }

    this.linesSignal.update(lines =>
      lines.map(l => (l.productId === product.id ? { ...l, quantity: l.quantity + 1 } : l))
    );
  }

  incrementLine(productId: string): void {
    const existing = this.linesSignal().find(l => l.productId === productId);
    if (!existing) return;
    this.linesSignal.update(lines =>
      lines.map(l => (l.productId === productId ? { ...l, quantity: l.quantity + 1 } : l))
    );
  }

  decrementProduct(productId: string): void {
    const existing = this.linesSignal().find(l => l.productId === productId);
    if (!existing) return;
    if (existing.quantity <= 1) {
      this.removeProduct(productId);
      return;
    }
    this.linesSignal.update(lines =>
      lines.map(l => (l.productId === productId ? { ...l, quantity: l.quantity - 1 } : l))
    );
  }

  removeProduct(productId: string): void {
    this.linesSignal.update(lines => lines.filter(l => l.productId !== productId));
  }

  clear(): void {
    this.linesSignal.set([]);
    this.note.set('');
  }

  toggleModifierOption(productId: string, groupId: number, optionId: number): void {
    this.linesSignal.update(lines =>
      lines.map(line => {
        if (line.productId !== productId) {
          return line;
        }

        const groups = Array.isArray(line.modifierGroups) ? line.modifierGroups : [];
        const group = groups.find(g => g.id === groupId);
        if (!group) {
          return line;
        }

        const option = Array.isArray(group.options) ? group.options.find(o => o.id === optionId) : undefined;
        if (!option) {
          return line;
        }

        const current = new Set<number>((line.selectedModifierOptionIds ?? []).filter(n => Number.isFinite(n)));

        const maxSelectable = group.maxSelectable ?? null;
        const isSingle = maxSelectable === 1;

        if (isSingle) {
          // Radio-like group: select only one option in the group.
          // Remove any other option belonging to this group.
          for (const opt of group.options ?? []) {
            current.delete(opt.id);
          }
          current.add(optionId);
        } else {
          if (current.has(optionId)) {
            current.delete(optionId);
          } else {
            if (typeof maxSelectable === 'number' && maxSelectable > 0) {
              const selectedInGroup = (group.options ?? []).filter(o => current.has(o.id)).length;
              if (selectedInGroup >= maxSelectable) {
                return line;
              }
            }
            current.add(optionId);
          }
        }

        const selectedIds = Array.from(current.values());
        const unitPriceCents = this.computeUnitPriceCents(line.baseUnitPriceCents, groups, selectedIds);

        return {
          ...line,
          selectedModifierOptionIds: selectedIds,
          unitPriceCents,
        };
      })
    );
  }

  private computeUnitPriceCents(baseUnitPriceCents: number, groups: MenuProduct['modifierGroups'], selectedIds: number[]): number {
    const selected = new Set<number>((selectedIds ?? []).filter(n => Number.isFinite(n)));
    const safeBase = Number.isFinite(baseUnitPriceCents) ? baseUnitPriceCents : 0;

    let totalPrice = safeBase;
    for (const group of groups ?? []) {
      for (const opt of group.options ?? []) {
        if (selected.has(opt.id)) {
          // Se l'opzione ha un prezzo proprio, usa quello; altrimenti usa il delta
          if (typeof opt.priceCents === 'number' && Number.isFinite(opt.priceCents)) {
            // Prezzo autonomo: sostituisce il prezzo base
            if (group.maxSelectable === 1) {
              totalPrice = opt.priceCents;
            } else {
              // Se multipla scelta, è un'aggiunta
              totalPrice += opt.priceCents;
            }
          } else if (typeof opt.priceDeltaCents === 'number' && Number.isFinite(opt.priceDeltaCents)) {
            // Delta: aggiunta al prezzo
            totalPrice += opt.priceDeltaCents;
          }
        }
      }
    }

    return totalPrice;
  }
}

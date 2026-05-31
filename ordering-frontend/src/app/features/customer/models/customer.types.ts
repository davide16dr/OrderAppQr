export type OrderStatus = 'RECEIVED' | 'DELIVERED';

export interface CustomerLocationContext {
  businessName: string;
  businessAvatarText?: string;
  businessLogoDataUrl?: string | null;

  locationTitle: string;
  locationSubtitle?: string;

  statusLabel: string;
  statusVariant: 'active' | 'inactive';
}

export interface MenuCategory {
  id: string;
  name: string;
  icon?: string;
}

export interface MenuProduct {
  id: string;
  name: string;
  description?: string;
  priceCents: number;
  imageUrl?: string;
  icon?: string;
  categoryId: string;
  modifierGroups?: ModifierGroup[];
}

export interface ModifierOption {
  id: number;
  name: string;
  priceDeltaCents?: number;
  priceCents?: number;
}

export interface ModifierGroup {
  id: number;
  name: string;
  minSelectable: number;
  maxSelectable?: number | null;
  required: boolean;
  options: ModifierOption[];
}

export interface CustomerMenuViewModel {
  context: CustomerLocationContext;
  categories: MenuCategory[];
  products: MenuProduct[];
}

export interface CartLine {
  productId: string;
  name: string;
  description?: string;
  baseUnitPriceCents: number;
  unitPriceCents: number;
  imageUrl?: string;
  icon?: string;
  quantity: number;

  modifierGroups?: ModifierGroup[];
  selectedModifierOptionIds?: number[];
}

export interface CustomerOrderItem {
  productId: string;
  name: string;
  quantity: number;
  unitPriceCents: number;
  modifierGroups?: {
    groupName: string;
    optionNames: string[];
  }[];
}

export interface CustomerOrderData {
  id: string;
  status: OrderStatus;
  items: CustomerOrderItem[];
  totalCents: number;
}

export function formatEuroFromCents(cents: number): string {
  const value = (cents ?? 0) / 100;
  return value.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' });
}

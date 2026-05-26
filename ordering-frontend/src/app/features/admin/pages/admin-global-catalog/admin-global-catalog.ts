import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

type ProductStatus = 'available' | 'unavailable';

interface MenuProduct {
  id: number;
  name: string;
  description: string;
  price: number;
  category: string;
  imageLabel: string;
  imageTone: 'amber' | 'blue' | 'emerald' | 'violet' | 'slate';
  status: ProductStatus;
  variants: number;
  extras: number;
  label: string;
}

@Component({
  selector: 'app-admin-global-catalog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="menu-page">
      <header class="hero">
        <div>
          <p class="eyebrow">Catalogo menu</p>
          <h1>Menu</h1>
          <p class="subtitle">Gestione completa del menu e dei prodotti</p>
        </div>

        <button type="button" class="add-button">
          <span>+</span>
          Aggiungi prodotto
        </button>
      </header>

      <section class="stats-grid">
        <article class="stat-card neutral">
          <span class="stat-label">Totale</span>
          <strong>{{ totalProducts }}</strong>
        </article>

        <article class="stat-card success">
          <span class="stat-label">Disponibili</span>
          <strong>{{ availableProducts }}</strong>
        </article>

        <article class="stat-card danger">
          <span class="stat-label">Non disponibili</span>
          <strong>{{ unavailableProducts }}</strong>
        </article>

        <article class="stat-card violet">
          <span class="stat-label">Con varianti</span>
          <strong>{{ withVariants }}</strong>
        </article>

        <article class="stat-card amber">
          <span class="stat-label">Con extra</span>
          <strong>{{ withExtras }}</strong>
        </article>
      </section>

      <section class="toolbar">
        <label class="search-box">
          <span aria-hidden="true">⌕</span>
          <input
            type="search"
            [value]="searchTerm"
            (input)="onSearchChange($any($event.target).value)"
            placeholder="Cerca prodotto..."
          />
        </label>

        <label class="select-box">
          <select [value]="selectedStatus" (change)="onStatusChange($any($event.target).value)">
            @for (option of statusOptions; track option.value) {
              <option [value]="option.value">{{ option.label }}</option>
            }
          </select>
        </label>
      </section>

      <nav class="category-tabs" aria-label="Categorie menu">
        @for (category of categories; track category) {
          <button
            type="button"
            class="tab"
            [class.active]="selectedCategory === category"
            (click)="onCategoryChange(category)">
            {{ category }}
          </button>
        }
      </nav>

      <section class="catalog-grid">
        @for (product of filteredProducts; track trackProduct($index, product)) {
          <article class="product-card">
            <div class="product-thumb tone-{{ product.imageTone }}">
              <span>{{ product.imageLabel }}</span>
            </div>

            <div class="product-content">
              <header class="product-head">
                <div>
                  <h2>{{ product.name }}</h2>
                  <p>{{ product.description }}</p>
                </div>

                <button type="button" class="more-button" aria-label="Azioni prodotto">⋮</button>
              </header>

              <div class="price-row">
                <strong>€{{ product.price | number: '1.2-2' }}</strong>

                <div class="tags">
                  <span class="tag category">{{ product.category }}</span>
                  @if (product.variants > 0) {
                    <span class="tag violet">{{ product.variants }} varianti</span>
                  }
                  @if (product.extras > 0) {
                    <span class="tag amber">{{ product.extras }} extra</span>
                  }
                  @if (product.status === 'unavailable') {
                    <span class="tag danger">Non disponibile</span>
                  }
                </div>
              </div>
            </div>
          </article>
        }
      </section>
    </section>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100%;
    }

    .menu-page {
      min-height: 100%;
      padding: 24px;
      background: #fafbff;
      color: #162033;
    }

    .hero {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;
    }

    .eyebrow {
      margin: 0 0 8px;
      font-size: 13px;
      font-weight: 700;
      color: #728199;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    h1 {
      margin: 0;
      font-size: clamp(30px, 3vw, 38px);
      line-height: 1.1;
      letter-spacing: -0.03em;
    }

    .subtitle {
      margin: 8px 0 0;
      color: #75839a;
      font-size: 15px;
    }

    .add-button {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      min-height: 44px;
      padding: 0 18px;
      border: 0;
      border-radius: 12px;
      background: linear-gradient(135deg, #2f6de0 0%, #305bd6 100%);
      color: #fff;
      font-weight: 700;
      box-shadow: 0 12px 24px rgba(47, 109, 224, 0.22);
      cursor: pointer;
    }

    .add-button span {
      font-size: 18px;
      line-height: 1;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: 10px;
      margin-bottom: 18px;
    }

    .stat-card {
      padding: 16px;
      border-radius: 16px;
      background: #fff;
      border: 1px solid #edf1f7;
      box-shadow: 0 8px 24px rgba(20, 35, 64, 0.04);
    }

    .stat-card strong {
      display: block;
      margin-top: 6px;
      font-size: 28px;
      line-height: 1;
      letter-spacing: -0.03em;
    }

    .stat-label {
      font-size: 13px;
      font-weight: 600;
    }

    .neutral { background: #f5f7fb; }
    .neutral .stat-label, .neutral strong { color: #1f2937; }
    .success { background: #e8fbef; }
    .success .stat-label, .success strong { color: #1c7c4a; }
    .danger { background: #fdeeee; }
    .danger .stat-label, .danger strong { color: #d13b3b; }
    .violet { background: #f4edff; }
    .violet .stat-label, .violet strong { color: #8b4ae0; }
    .amber { background: #fff8e8; }
    .amber .stat-label, .amber strong { color: #c56a08; }

    .toolbar {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 180px;
      gap: 12px;
      margin-bottom: 14px;
    }

    .search-box,
    .select-box {
      display: flex;
      align-items: center;
      gap: 10px;
      min-height: 50px;
      border-radius: 14px;
      background: #fff;
      border: 1px solid #e3e8f1;
      box-shadow: 0 8px 18px rgba(20, 35, 64, 0.04);
      padding: 0 14px;
    }

    .search-box span {
      color: #94a3b8;
      font-size: 18px;
    }

    .search-box input,
    .select-box select {
      width: 100%;
      border: 0;
      outline: none;
      background: transparent;
      font-size: 15px;
      color: #162033;
    }

    .category-tabs {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      padding: 10px 0 18px;
      border-bottom: 1px solid #edf1f7;
      margin-bottom: 22px;
    }

    .tab {
      border: 0;
      border-radius: 12px;
      padding: 10px 14px;
      background: #fff;
      color: #67768c;
      font-weight: 700;
      box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
      cursor: pointer;
    }

    .tab.active {
      background: #eef4ff;
      color: #2f6de0;
      box-shadow: inset 0 0 0 1px rgba(47, 109, 224, 0.12);
    }

    .catalog-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
    }

    .product-card {
      display: flex;
      align-items: stretch;
      gap: 14px;
      padding: 14px;
      border-radius: 18px;
      background: #fff;
      border: 1px solid #dfe7f1;
      box-shadow: 0 8px 24px rgba(20, 35, 64, 0.05);
      min-height: 104px;
    }

    .product-thumb {
      width: 96px;
      min-width: 96px;
      border-radius: 14px;
      display: grid;
      place-items: center;
      font-size: 17px;
      font-weight: 800;
      color: #fff;
      letter-spacing: 0.03em;
    }

    .tone-amber { background: linear-gradient(135deg, #9a5c13, #f0a11c); }
    .tone-blue { background: linear-gradient(135deg, #2446a8, #4fa0ff); }
    .tone-emerald { background: linear-gradient(135deg, #127f57, #2cc58c); }
    .tone-violet { background: linear-gradient(135deg, #6f45e8, #b37dff); }
    .tone-slate { background: linear-gradient(135deg, #6b7280, #9aa3b2); }

    .product-content {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .product-head {
      display: flex;
      justify-content: space-between;
      gap: 10px;
    }

    .product-head h2 {
      margin: 0;
      font-size: 18px;
      line-height: 1.1;
      letter-spacing: -0.02em;
    }

    .product-head p {
      margin: 4px 0 0;
      color: #708198;
      font-size: 14px;
      line-height: 1.4;
    }

    .more-button {
      width: 28px;
      height: 28px;
      border: 0;
      border-radius: 999px;
      background: transparent;
      color: #8a97aa;
      font-size: 22px;
      line-height: 1;
      cursor: pointer;
      flex: none;
    }

    .price-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
      margin-top: auto;
    }

    .price-row strong {
      color: #1c4bd8;
      font-size: 20px;
      letter-spacing: -0.02em;
    }

    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: flex-end;
    }

    .tag {
      display: inline-flex;
      align-items: center;
      min-height: 24px;
      padding: 0 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      white-space: nowrap;
    }

    .tag.category {
      background: #eef3fb;
      color: #5f6f86;
    }

    .tag.violet {
      background: #f0e7ff;
      color: #8740dd;
    }

    .tag.amber {
      background: #fff0d9;
      color: #cc7507;
    }

    .tag.danger {
      background: #ffe5e5;
      color: #d13b3b;
    }

    @media (max-width: 1200px) {
      .stats-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .catalog-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 840px) {
      .menu-page {
        padding: 18px;
      }

      .hero,
      .toolbar {
        grid-template-columns: 1fr;
        display: grid;
      }

      .add-button {
        justify-self: start;
      }

      .catalog-grid {
        grid-template-columns: 1fr;
      }

      .product-card {
        flex-direction: column;
      }

      .product-thumb {
        width: 100%;
        min-width: 0;
        min-height: 110px;
      }

      .price-row {
        align-items: flex-start;
      }

      .tags {
        justify-content: flex-start;
      }
    }
  `]
})
export class AdminGlobalCatalog {
  readonly categories = ['Tutti', 'Bevande', 'Cocktail', 'Cibo', 'Snack', 'Dolci'];

  readonly statusOptions = [
    { value: 'ALL', label: 'Tutti i prodotti' },
    { value: 'available', label: 'Disponibili' },
    { value: 'unavailable', label: 'Non disponibili' }
  ] as const;

  readonly products: MenuProduct[] = [
    {
      id: 1,
      name: 'Aperol Spritz',
      description: 'Il classico cocktail italiano',
      price: 8,
      category: 'Cocktail',
      imageLabel: 'A',
      imageTone: 'amber',
      status: 'available',
      variants: 2,
      extras: 2,
      label: 'cocktails'
    },
    {
      id: 2,
      name: 'Club Sandwich',
      description: 'Toast con pollo, bacon, lattuga e pomodoro',
      price: 12,
      category: 'Cibo',
      imageLabel: 'C',
      imageTone: 'blue',
      status: 'available',
      variants: 0,
      extras: 2,
      label: 'food'
    },
    {
      id: 3,
      name: 'Coca Cola',
      description: 'Lattina 33cl',
      price: 4,
      category: 'Bevande',
      imageLabel: 'C',
      imageTone: 'slate',
      status: 'available',
      variants: 2,
      extras: 0,
      label: 'drinks'
    },
    {
      id: 4,
      name: 'Gelato Artigianale',
      description: 'Due gusti a scelta',
      price: 6,
      category: 'Dolci',
      imageLabel: 'G',
      imageTone: 'emerald',
      status: 'available',
      variants: 0,
      extras: 2,
      label: 'desserts'
    },
    {
      id: 5,
      name: 'Mojito',
      description: 'Rum, menta, lime e zucchero',
      price: 9,
      category: 'Cocktail',
      imageLabel: 'M',
      imageTone: 'violet',
      status: 'unavailable',
      variants: 0,
      extras: 0,
      label: 'cocktails'
    }
  ];

  searchTerm = '';
  selectedCategory = 'Tutti';
  selectedStatus: 'ALL' | ProductStatus = 'ALL';

  get filteredProducts(): MenuProduct[] {
    const search = this.searchTerm.trim().toLowerCase();

    return this.products.filter((product) => {
      const matchesSearch = !search || product.name.toLowerCase().includes(search) || product.description.toLowerCase().includes(search);
      const matchesCategory = this.selectedCategory === 'Tutti' || product.category === this.selectedCategory;
      const matchesStatus = this.selectedStatus === 'ALL' || product.status === this.selectedStatus;

      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  get totalProducts(): number {
    return this.products.length;
  }

  get availableProducts(): number {
    return this.products.filter((product) => product.status === 'available').length;
  }

  get unavailableProducts(): number {
    return this.products.filter((product) => product.status === 'unavailable').length;
  }

  get withVariants(): number {
    return this.products.filter((product) => product.variants > 0).length;
  }

  get withExtras(): number {
    return this.products.filter((product) => product.extras > 0).length;
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
  }

  onCategoryChange(category: string): void {
    this.selectedCategory = category;
  }

  onStatusChange(value: string): void {
    this.selectedStatus = value as 'ALL' | ProductStatus;
  }

  trackProduct(index: number, product: MenuProduct): number {
    return product.id;
  }

}

import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, retry, takeUntil, timeout } from 'rxjs/operators';
import { CreateTenantProductPayload, DashboardService, TenantProduct, TenantProductDetails, UpdateTenantProductPayload } from '../../services/dashboard.service';
import { CategoryService, TenantCategory } from '../../services/category.service';

type ProductAvailability = 'available' | 'unavailable';
type Department = 'BAR' | 'KITCHEN' | 'SERVICE' | 'GENERIC';
type CreateTab = 'basic' | 'variants' | 'extras';

interface MenuProduct {
  id: number;
  name: string;
  description: string;
  price: number;
  category: string;
  imageLabel: string;
  imageTone: 'amber' | 'blue' | 'emerald' | 'violet' | 'slate';
  status: ProductAvailability;
  variants: number;
  extras: number;
  department: Department;
  vatRate: number;
  availableForOrder: boolean;
  sku?: string;
  imageUrl?: string;
}

interface CreateProductForm {
  name: string;
  description: string;
  price: number | null;
  category: string;
  availableForOrder: boolean;
  variants: Array<{ name: string; priceDelta: number }>;
  extras: Array<{ name: string; priceDelta: number }>;
}

@Component({
  selector: 'app-top-products',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="menu-page">
      <header class="hero">
        <div>
          <p class="eyebrow">Il tuo menu</p>
          <h2>Prodotti</h2>
          <p class="subtitle">Vedi e gestisci i prodotti del tuo menu</p>
        </div>

        <button type="button" class="add-button" (click)="openCreateProductModal()">
          <span>+</span>
          Aggiungi prodotto
        </button>
      </header>

      @if (isLoading && products.length === 0) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Caricamento prodotti...</p>
        </div>
      } @else {
        @if (hasError) {
          <div class="warning-banner">
            Errore nel caricamento prodotti.
            <button type="button" (click)="reloadProducts()">Riprova</button>
          </div>
        }

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
          @if (filteredProducts.length === 0) {
            <article class="empty-state">Nessun prodotto trovato per i filtri selezionati.</article>
          } @else {
            @for (product of filteredProducts; track trackProduct($index, product)) {
              <article class="product-card">
                <div class="product-thumb tone-{{ product.imageTone }}">
                  @if (product.imageUrl) {
                    <img [src]="product.imageUrl" [alt]="product.name" class="thumb-image" />
                  } @else {
                    <span>{{ product.imageLabel }}</span>
                  }
                </div>

                <div class="product-content">
                  <header class="product-head">
                    <div>
                      <h3>{{ product.name }}</h3>
                      <p>{{ product.description }}</p>
                    </div>

                    <button type="button" class="more-button" aria-label="Azioni prodotto" (click)="openProductActions(product)">⋮</button>
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
          }
        </section>
      }

      @if (actionsProduct) {
        <div class="modal-backdrop" (click)="closeProductActions()">
          <div class="action-sheet" (click)="$event.stopPropagation()">
            <header>
              <h3>{{ actionsProduct.name }}</h3>
              <p>{{ actionsProduct.category }} · €{{ actionsProduct.price | number: '1.2-2' }}</p>
            </header>

            <div class="actions">
              <button type="button" class="primary" (click)="openEditProductModal(actionsProduct)">Modifica</button>
              <button type="button" class="danger" (click)="askDeleteProduct(actionsProduct)">Elimina</button>
              <button type="button" class="neutral" (click)="closeProductActions()">Annulla</button>
            </div>
          </div>
        </div>
      }

      @if (isProductModalOpen) {
        <div class="modal-backdrop" (click)="closeProductModal()">
          <div class="create-modal" (click)="$event.stopPropagation()">
            <header class="modal-header">
              <div class="title-row">
                <div class="title-icon">{{ productModalMode === 'create' ? '+' : '✎' }}</div>
                <div>
                  <h3>{{ productModalMode === 'create' ? 'Nuovo prodotto' : 'Modifica prodotto' }}</h3>
                  <p>{{ productModalMode === 'create' ? 'Aggiungi un nuovo prodotto al menu' : 'Aggiorna i dati del prodotto' }}</p>
                </div>
              </div>
              <button type="button" class="close-btn" (click)="closeProductModal()" aria-label="Chiudi">×</button>
            </header>

            <div class="modal-tabs" role="tablist" aria-label="Sezioni prodotto">
              <button type="button" [class.active]="activeCreateTab === 'basic'" (click)="setCreateTab('basic')">Info Base</button>
              <button type="button" [class.active]="activeCreateTab === 'variants'" (click)="setCreateTab('variants')">Varianti</button>
              <button type="button" [class.active]="activeCreateTab === 'extras'" (click)="setCreateTab('extras')">Extra</button>
            </div>

            <section class="modal-body">
              @if (createErrorMessage) {
                <div class="warning-banner" style="margin-bottom: 16px;">
                  {{ createErrorMessage }}
                </div>
              }

              @if (productModalMode === 'edit' && isLoadingProductDetails) {
                <div class="warning-banner" style="margin-bottom: 16px;">
                  Caricamento varianti/extra...
                </div>
              }

              @if (activeCreateTab === 'basic') {
                <div class="field-group">
                  <label>Nome prodotto <span>*</span></label>
                  <input
                    type="text"
                    [value]="createForm.name"
                    (input)="onCreateNameChange($any($event.target).value)"
                    placeholder="es: Aperol Spritz, Club Sandwich..."
                  />
                </div>

                <div class="field-group">
                  <label>Descrizione</label>
                  <textarea
                    rows="3"
                    [value]="createForm.description"
                    (input)="onCreateDescriptionChange($any($event.target).value)"
                    placeholder="Breve descrizione del prodotto...">
                  </textarea>
                </div>

                <div class="field-grid two">
                  <div class="field-group">
                    <label>Prezzo (€) <span>*</span></label>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      [value]="createForm.price ?? ''"
                      (input)="onCreatePriceChange($any($event.target).value)"
                      placeholder="8.00"
                    />
                  </div>

                  <div class="field-group">
                    <label>Categoria <span>*</span></label>
                    <select [value]="createForm.category" (change)="onCreateCategoryChange($any($event.target).value)">
                      <option value="">Seleziona</option>
                      @for (category of formCategories; track category) {
                        <option [value]="category">{{ category }}</option>
                      }
                    </select>
                  </div>
                </div>

                <div class="field-grid two">
                  <div class="field-group">
                    <label>Immagine prodotto</label>
                    <input
                      #imagePicker
                      type="file"
                      accept="image/*"
                      class="hidden-file-input"
                      (change)="onImagePicked($event)"
                    />

                    <div
                      class="image-dropzone"
                      [class.drag-over]="isImageDragOver"
                      (dragover)="onImageDragOver($event)"
                      (dragleave)="onImageDragLeave($event)"
                      (drop)="onImageDrop($event)">
                      @if (!createImagePreview) {
                        <p>Trascina qui un'immagine oppure</p>
                        <button type="button" class="pick-image-btn" (click)="imagePicker.click()">Scegli da dispositivo</button>
                        <small>Supporta caricamento da computer o telefono</small>
                      } @else {
                        <img [src]="createImagePreview" alt="Anteprima prodotto" class="image-preview" />
                        <button type="button" class="pick-image-btn" (click)="imagePicker.click()">Cambia immagine</button>
                      }
                    </div>
                  </div>
                </div>

                <div class="toggle-row">
                  <div>
                    <label>Disponibile per ordini</label>
                    <p>Mappa il campo DB <code>available_for_order</code></p>
                  </div>
                  <button
                    type="button"
                    class="toggle"
                    [class.on]="createForm.availableForOrder"
                    (click)="toggleAvailableForOrder()"
                    [attr.aria-pressed]="createForm.availableForOrder">
                    <span></span>
                  </button>
                </div>
              }

              @if (activeCreateTab === 'variants') {
                <div class="list-builder">
                  <h4>Aggiungi variante</h4>
                  <div class="field-grid two compact">
                    <input
                      type="text"
                      [value]="variantDraftName"
                      (input)="onVariantDraftNameChange($any($event.target).value)"
                      placeholder="Nome variante"
                      [disabled]="isLoadingProductDetails"
                    />
                    <input
                      type="number"
                      step="0.01"
                      [value]="variantDraftPrice"
                      (input)="onVariantDraftPriceChange($any($event.target).value)"
                      placeholder="0"
                      [disabled]="isLoadingProductDetails"
                    />
                  </div>
                  <button type="button" class="add-line-btn" [disabled]="!canAddVariant || isLoadingProductDetails" (click)="addVariant()">+ Aggiungi</button>

                  @if (createForm.variants.length > 0) {
                    <div class="line-list">
                      @for (item of createForm.variants; track item.name + '-' + $index; let i = $index) {
                        <div class="line-item">
                          <div>
                            <strong>{{ item.name }}</strong>
                            <p>{{ item.priceDelta >= 0 ? '+' : '' }}€{{ item.priceDelta | number: '1.2-2' }}</p>
                          </div>
                          <button type="button" class="delete-btn" [disabled]="isLoadingProductDetails" (click)="removeVariant(i)" aria-label="Rimuovi variante">🗑</button>
                        </div>
                      }
                    </div>
                  }
                </div>
              }

              @if (activeCreateTab === 'extras') {
                <div class="list-builder">
                  <h4>Aggiungi extra</h4>
                  <div class="field-grid two compact">
                    <input
                      type="text"
                      [value]="extraDraftName"
                      (input)="onExtraDraftNameChange($any($event.target).value)"
                      placeholder="Nome extra"
                      [disabled]="isLoadingProductDetails"
                    />
                    <input
                      type="number"
                      step="0.01"
                      [value]="extraDraftPrice"
                      (input)="onExtraDraftPriceChange($any($event.target).value)"
                      placeholder="0"
                      [disabled]="isLoadingProductDetails"
                    />
                  </div>
                  <button type="button" class="add-line-btn" [disabled]="!canAddExtra || isLoadingProductDetails" (click)="addExtra()">+ Aggiungi</button>

                  @if (createForm.extras.length > 0) {
                    <div class="line-list">
                      @for (item of createForm.extras; track item.name + '-' + $index; let i = $index) {
                        <div class="line-item">
                          <div>
                            <strong>{{ item.name }}</strong>
                            <p>{{ item.priceDelta >= 0 ? '+' : '' }}€{{ item.priceDelta | number: '1.2-2' }}</p>
                          </div>
                          <button type="button" class="delete-btn" [disabled]="isLoadingProductDetails" (click)="removeExtra(i)" aria-label="Rimuovi extra">🗑</button>
                        </div>
                      }
                    </div>
                  }
                </div>
              }
            </section>

            <footer class="modal-footer">
              <button type="button" class="cancel" [disabled]="isCreating" (click)="closeProductModal()">Annulla</button>
              <button type="button" class="confirm" [disabled]="isCreating || !canCreateProduct" (click)="saveProduct()">
                @if (isCreating) {
                  {{ productModalMode === 'create' ? 'Creazione...' : 'Salvataggio...' }}
                } @else {
                  {{ productModalMode === 'create' ? 'Crea prodotto' : 'Salva modifiche' }}
                }
              </button>
            </footer>
          </div>
        </div>
      }

      @if (productToDelete) {
        <div class="modal-backdrop" (click)="cancelDeleteProduct()">
          <div class="confirm-modal" (click)="$event.stopPropagation()">
            <header>
              <h3>Elimina prodotto</h3>
              <p>Vuoi eliminare <strong>{{ productToDelete.name }}</strong>? Il prodotto verrà disattivato e non comparirà più nel menu.</p>
            </header>

            @if (deleteErrorMessage) {
              <div class="warning-banner" style="margin-bottom: 12px;">
                {{ deleteErrorMessage }}
              </div>
            }

            <footer>
              <button type="button" class="neutral" [disabled]="isDeleting" (click)="cancelDeleteProduct()">Annulla</button>
              <button type="button" class="danger" [disabled]="isDeleting" (click)="confirmDeleteProduct()">
                {{ isDeleting ? 'Eliminazione...' : 'Elimina' }}
              </button>
            </footer>
          </div>
        </div>
      }
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

    h2 {
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

    .loading-state {
      min-height: 320px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      border: 1px solid #e3e8f1;
      border-radius: 16px;
      background: #ffffff;
      box-shadow: 0 8px 24px rgba(20, 35, 64, 0.04);
    }

    .loading-state .spinner {
      width: 34px;
      height: 34px;
      border-radius: 999px;
      border: 3px solid #dbe7fb;
      border-top-color: #2f6de0;
      animation: spin 0.9s linear infinite;
    }

    .loading-state p {
      margin: 0;
      color: #64748b;
      font-weight: 600;
    }

    .warning-banner {
      margin-bottom: 14px;
      border-radius: 12px;
      border: 1px solid #fed7aa;
      background: #fff7ed;
      color: #b45309;
      padding: 10px 12px;
      font-size: 13px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
    }

    .warning-banner button {
      border: 1px solid #fdba74;
      background: #fff;
      color: #9a3412;
      border-radius: 8px;
      min-height: 30px;
      padding: 0 10px;
      font-weight: 700;
      cursor: pointer;
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

    .empty-state {
      grid-column: 1 / -1;
      border: 1px dashed #cfd9e8;
      border-radius: 14px;
      background: #ffffff;
      color: #64748b;
      min-height: 140px;
      display: grid;
      place-items: center;
      font-weight: 600;
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
      overflow: hidden;
    }

    .thumb-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
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

    .product-head h3 {
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

    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 90;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      background: rgba(15, 23, 42, 0.5);
      backdrop-filter: blur(8px);
    }

    .action-sheet,
    .confirm-modal {
      width: min(100%, 420px);
      border-radius: 18px;
      background: #fff;
      box-shadow: 0 32px 90px rgba(15, 23, 42, 0.35);
      overflow: hidden;
      border: 1px solid rgba(226, 232, 240, 0.9);
    }

    .action-sheet header,
    .confirm-modal header {
      padding: 18px 18px 12px;
      border-bottom: 1px solid #ecf1f7;
    }

    .action-sheet h3,
    .confirm-modal h3 {
      margin: 0;
      font-size: 18px;
      color: #0f172a;
      letter-spacing: -0.02em;
    }

    .action-sheet p,
    .confirm-modal p {
      margin: 8px 0 0;
      color: #64748b;
      font-size: 13px;
      line-height: 1.35;
    }

    .action-sheet .actions {
      display: grid;
      gap: 10px;
      padding: 14px 18px 18px;
    }

    .action-sheet button,
    .confirm-modal button {
      min-height: 42px;
      border-radius: 12px;
      border: 1px solid #e2e8f0;
      background: #fff;
      font-weight: 800;
      cursor: pointer;
    }

    .action-sheet button.primary {
      background: #2563eb;
      border-color: #2563eb;
      color: #fff;
    }

    .action-sheet button.danger,
    .confirm-modal button.danger {
      background: #fee2e2;
      border-color: #fecaca;
      color: #b91c1c;
    }

    .action-sheet button.neutral,
    .confirm-modal button.neutral {
      background: #f8fafc;
      border-color: #e2e8f0;
      color: #0f172a;
    }

    .action-sheet button:disabled,
    .confirm-modal button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .confirm-modal footer {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      padding: 14px 18px 18px;
    }

    .create-modal {
      width: min(100%, 820px);
      max-height: 88vh;
      border-radius: 22px;
      background: #fff;
      box-shadow: 0 32px 90px rgba(15, 23, 42, 0.35);
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .modal-header {
      padding: 18px 20px;
      border-bottom: 1px solid #ecf1f7;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
    }

    .title-row {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .title-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: grid;
      place-items: center;
      background: #dbeafe;
      color: #2563eb;
      font-size: 22px;
      font-weight: 700;
    }

    .title-row h3 {
      margin: 0;
      font-size: 22px;
      color: #0f172a;
    }

    .title-row p {
      margin: 4px 0 0;
      color: #64748b;
      font-size: 13px;
    }

    .close-btn {
      width: 34px;
      height: 34px;
      border: 0;
      border-radius: 10px;
      background: transparent;
      color: #64748b;
      font-size: 25px;
      line-height: 1;
      cursor: pointer;
    }

    .close-btn:hover {
      background: #f1f5f9;
      color: #0f172a;
    }

    .modal-tabs {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #f5f7fb;
      margin: 14px 20px 0;
      padding: 4px;
      border-radius: 12px;
    }

    .modal-tabs button {
      border: 0;
      border-radius: 9px;
      background: transparent;
      color: #66758d;
      font-weight: 700;
      font-size: 13px;
      padding: 8px 12px;
      cursor: pointer;
    }

    .modal-tabs button.active {
      background: #fff;
      color: #1e293b;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.12);
    }

    .modal-body {
      flex: 1;
      overflow: auto;
      padding: 18px 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .field-group label {
      font-size: 13px;
      font-weight: 700;
      color: #0f172a;
    }

    .field-group label span {
      color: #dc2626;
    }

    .field-group input,
    .field-group select,
    .field-group textarea {
      border: 1px solid #d8e2ef;
      border-radius: 12px;
      padding: 10px 12px;
      font-size: 14px;
      color: #162033;
      outline: none;
      background: #fff;
    }

    .field-group textarea {
      resize: vertical;
      min-height: 72px;
    }

    .field-group small {
      color: #718096;
      font-size: 12px;
    }

    .field-grid.two {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    .hidden-file-input {
      display: none;
    }

    .image-dropzone {
      border: 2px dashed #cfd9e8;
      border-radius: 16px;
      background: #f8fbff;
      min-height: 150px;
      padding: 14px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      text-align: center;
    }

    .image-dropzone p {
      margin: 0;
      color: #334155;
      font-weight: 600;
    }

    .image-dropzone small {
      color: #64748b;
      font-size: 12px;
    }

    .image-dropzone.drag-over {
      border-color: #2563eb;
      background: #eaf2ff;
    }

    .pick-image-btn {
      border: 1px solid #c9d7f2;
      background: #ffffff;
      border-radius: 10px;
      min-height: 36px;
      padding: 0 12px;
      color: #1d4ed8;
      font-weight: 700;
      cursor: pointer;
    }

    .image-preview {
      max-height: 120px;
      max-width: 100%;
      border-radius: 12px;
      object-fit: cover;
      box-shadow: 0 8px 18px rgba(20, 35, 64, 0.14);
    }

    .field-grid.two.compact input {
      min-height: 44px;
      border: 1px solid #d7dee8;
      border-radius: 16px;
      padding: 0 16px;
      font-size: 16px;
      background: #ffffff;
    }

    .list-builder {
      border-radius: 18px;
      background: #f4f6fa;
      padding: 14px;
      border: 1px solid #e7edf6;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .list-builder h4 {
      margin: 0;
      font-size: 40px;
      font-size: clamp(28px, 3vw, 42px);
      line-height: 1;
      letter-spacing: -0.03em;
      color: #162033;
      font-weight: 800;
    }

    .add-line-btn {
      min-height: 48px;
      border: 1px solid #d7dee8;
      border-radius: 20px;
      background: #ffffff;
      font-size: 20px;
      font-weight: 700;
      color: #111827;
      cursor: pointer;
    }

    .add-line-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .line-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-top: 2px;
    }

    .line-item {
      border: 1px solid #d7dee8;
      border-radius: 22px;
      background: #fff;
      padding: 14px 18px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .line-item strong {
      display: block;
      font-size: 22px;
      line-height: 1.1;
      color: #172034;
    }

    .line-item p {
      margin: 4px 0 0;
      font-size: 40px;
      font-size: clamp(26px, 2.6vw, 40px);
      line-height: 1;
      color: #64748b;
      letter-spacing: -0.03em;
    }

    .delete-btn {
      width: 38px;
      height: 38px;
      border: 0;
      background: transparent;
      color: #ef4444;
      font-size: 20px;
      cursor: pointer;
      border-radius: 10px;
    }

    .delete-btn:hover {
      background: #fee2e2;
    }

    .toggle-row {
      margin-top: 2px;
      border: 1px solid #e4eaf3;
      border-radius: 14px;
      background: #f8fafc;
      padding: 12px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .toggle-row label {
      font-size: 14px;
      font-weight: 700;
      color: #1f2937;
    }

    .toggle-row p {
      margin: 4px 0 0;
      font-size: 12px;
      color: #64748b;
    }

    .toggle {
      width: 44px;
      height: 24px;
      border: 0;
      border-radius: 999px;
      background: #cbd5e1;
      padding: 2px;
      cursor: pointer;
      transition: background 0.2s ease;
    }

    .toggle span {
      display: block;
      width: 20px;
      height: 20px;
      border-radius: 999px;
      background: #fff;
      transition: transform 0.2s ease;
    }

    .toggle.on {
      background: #2563eb;
    }

    .toggle.on span {
      transform: translateX(20px);
    }

    .modal-footer {
      border-top: 1px solid #ecf1f7;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .modal-footer button {
      flex: 1;
      min-height: 42px;
      border-radius: 12px;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      border: 0;
    }

    .modal-footer .cancel {
      background: #fff;
      border: 1px solid #d6dfeb;
      color: #334155;
    }

    .modal-footer .confirm {
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #fff;
      box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
    }

    .modal-footer .confirm:disabled {
      opacity: 0.45;
      cursor: not-allowed;
      box-shadow: none;
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

      .field-grid.two {
        grid-template-columns: 1fr;
      }

      .create-modal {
        max-height: 92vh;
      }
    }

    @keyframes spin {
      from {
        transform: rotate(0deg);
      }
      to {
        transform: rotate(360deg);
      }
    }
  `]
})
export class TopProductsComponent implements OnInit, OnDestroy {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly categoryService: CategoryService = inject(CategoryService);

  readonly loadedCategories = signal<TenantCategory[]>([]);
  readonly categoriesLoading = signal(false);

  readonly statusOptions = [
    { value: 'ALL', label: 'Tutti i prodotti' },
    { value: 'available', label: 'Disponibili' },
    { value: 'unavailable', label: 'Non disponibili' }
  ] as const;

  products: MenuProduct[] = [];

  get categories(): string[] {
    const cats = this.loadedCategories();
    return cats.length > 0 ? ['Tutti', ...cats.map(c => c.name)] : ['Tutti'];
  }

  get formCategories(): string[] {
    return this.loadedCategories().map(c => c.name);
  }
  readonly tones: Array<MenuProduct['imageTone']> = ['amber', 'blue', 'emerald', 'violet', 'slate'];

  searchTerm = '';
  selectedCategory = 'Tutti';
  selectedStatus: 'ALL' | ProductAvailability = 'ALL';

  actionsProduct: MenuProduct | null = null;
  productToDelete: MenuProduct | null = null;

  isProductModalOpen = false;
  productModalMode: 'create' | 'edit' = 'create';
  editingProductId: number | null = null;

  activeCreateTab: CreateTab = 'basic';
  variantDraftName = '';
  variantDraftPrice: number = 0;
  extraDraftName = '';
  extraDraftPrice: number = 0;
  isImageDragOver = false;
  createImagePreview: string | null = null;
  createForm: CreateProductForm = this.buildEmptyCreateForm();
  isCreating = false;
  isDeleting = false;
  createErrorMessage: string | null = null;
  deleteErrorMessage: string | null = null;
  isLoading = true;
  hasError = false;

  isLoadingProductDetails = false;
  hasLoadedProductDetails = false;

  private readonly destroy$ = new Subject<void>();

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts(true);
  }

  private loadCategories(): void {
    this.categoriesLoading.set(true);
    this.categoryService
      .getCategories()
      .pipe(
        timeout(8000),
        finalize(() => this.categoriesLoading.set(false)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (categories) => {
          this.loadedCategories.set(categories.sort((a, b) => a.displayOrder - b.displayOrder));
          this.cdr.markForCheck();
        },
        error: () => {
          console.error('Error loading categories');
          this.loadedCategories.set([]);
          this.cdr.markForCheck();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredProducts(): MenuProduct[] {
    const search = this.searchTerm.trim().toLowerCase();

    return this.products.filter((product) => {
      const matchesSearch = !search || product.name.toLowerCase().includes(search) || product.description.toLowerCase().includes(search);
      const matchesCategory = this.selectedCategory === 'Tutti' || product.category === this.selectedCategory;
      const matchesStatus = this.selectedStatus === 'ALL' || product.status === this.selectedStatus;

      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  get canCreateProduct(): boolean {
    return Boolean(this.createForm.name.trim())
      && this.createForm.price !== null
      && this.createForm.price >= 0
      && Boolean(this.createForm.category);
  }

  get canAddVariant(): boolean {
    return this.variantDraftName.trim().length > 0;
  }

  get canAddExtra(): boolean {
    return this.extraDraftName.trim().length > 0;
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
    this.selectedStatus = value as 'ALL' | ProductAvailability;
  }

  reloadProducts(): void {
    this.loadProducts(true);
  }

  openProductActions(product: MenuProduct): void {
    this.actionsProduct = product;
  }

  closeProductActions(): void {
    this.actionsProduct = null;
  }

  openEditProductModal(product: MenuProduct): void {
    this.closeProductActions();

    this.productModalMode = 'edit';
    this.editingProductId = product.id;
    this.isProductModalOpen = true;

    this.activeCreateTab = 'basic';
    this.createForm = {
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      category: product.category === 'Senza categoria' ? '' : product.category,
      availableForOrder: product.availableForOrder,
      variants: [],
      extras: []
    };

    this.variantDraftName = '';
    this.variantDraftPrice = 0;
    this.extraDraftName = '';
    this.extraDraftPrice = 0;

    this.hasLoadedProductDetails = false;

    this.createImagePreview = product.imageUrl ?? null;
    this.isImageDragOver = false;
    this.createErrorMessage = null;

    this.loadProductDetails(product.id);
  }

  private loadProductDetails(productId: number): void {
    this.isLoadingProductDetails = true;

    this.dashboardService.getTenantProductDetails(productId)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoadingProductDetails = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (details: TenantProductDetails) => {
          this.hasLoadedProductDetails = true;
          this.createForm = {
            ...this.createForm,
            variants: Array.isArray(details.variants) ? details.variants : [],
            extras: Array.isArray(details.extras) ? details.extras : []
          };
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          console.error('Error loading product details:', err);
          // Non blocchiamo l'edit: lasciamo liste vuote.
        }
      });
  }

  askDeleteProduct(product: MenuProduct): void {
    this.closeProductActions();
    this.productToDelete = product;
    this.deleteErrorMessage = null;
  }

  cancelDeleteProduct(): void {
    if (this.isDeleting) {
      return;
    }
    this.productToDelete = null;
    this.deleteErrorMessage = null;
  }

  openCreateProductModal(): void {
    this.productModalMode = 'create';
    this.editingProductId = null;
    this.isProductModalOpen = true;
    this.activeCreateTab = 'basic';
    this.createForm = this.buildEmptyCreateForm();
    this.variantDraftName = '';
    this.variantDraftPrice = 0;
    this.extraDraftName = '';
    this.extraDraftPrice = 0;
    this.isImageDragOver = false;
    this.createImagePreview = null;
    this.createErrorMessage = null;
    this.isLoadingProductDetails = false;
    this.hasLoadedProductDetails = false;
  }

  closeProductModal(force = false): void {
    if (this.isCreating && !force) {
      return;
    }

    this.isProductModalOpen = false;
    this.productModalMode = 'create';
    this.editingProductId = null;
    this.createForm = this.buildEmptyCreateForm();
    this.variantDraftName = '';
    this.variantDraftPrice = 0;
    this.extraDraftName = '';
    this.extraDraftPrice = 0;
    this.isImageDragOver = false;
    this.createImagePreview = null;
    this.activeCreateTab = 'basic';
    this.createErrorMessage = null;
    this.isLoadingProductDetails = false;
    this.hasLoadedProductDetails = false;
    this.cdr.markForCheck();
  }

  setCreateTab(tab: CreateTab): void {
    this.activeCreateTab = tab;
  }

  onCreateNameChange(value: string): void {
    this.createForm = { ...this.createForm, name: value };
  }

  onCreateDescriptionChange(value: string): void {
    this.createForm = { ...this.createForm, description: value };
  }

  onCreatePriceChange(value: string): void {
    const parsed = value === '' ? null : Number(value);
    this.createForm = { ...this.createForm, price: Number.isFinite(parsed) ? parsed : null };
  }

  onCreateCategoryChange(value: string): void {
    this.createForm = { ...this.createForm, category: value };
  }

  onImageDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isImageDragOver = true;
  }

  onImageDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isImageDragOver = false;
  }

  onImageDrop(event: DragEvent): void {
    event.preventDefault();
    this.isImageDragOver = false;

    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.setPickedImage(file);
    }
  }

  onImagePicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.setPickedImage(file);
    }
  }

  toggleAvailableForOrder(): void {
    this.createForm = { ...this.createForm, availableForOrder: !this.createForm.availableForOrder };
  }

  onVariantDraftNameChange(value: string): void {
    this.variantDraftName = value;
  }

  onVariantDraftPriceChange(value: string): void {
    const parsed = Number(value);
    this.variantDraftPrice = Number.isFinite(parsed) ? parsed : 0;
  }

  addVariant(): void {
    if (!this.canAddVariant) {
      return;
    }

    this.createForm = {
      ...this.createForm,
      variants: [
        ...this.createForm.variants,
        {
          name: this.variantDraftName.trim(),
          priceDelta: this.variantDraftPrice
        }
      ]
    };

    this.variantDraftName = '';
    this.variantDraftPrice = 0;
  }

  removeVariant(index: number): void {
    this.createForm = {
      ...this.createForm,
      variants: this.createForm.variants.filter((_, i) => i !== index)
    };
  }

  onExtraDraftNameChange(value: string): void {
    this.extraDraftName = value;
  }

  onExtraDraftPriceChange(value: string): void {
    const parsed = Number(value);
    this.extraDraftPrice = Number.isFinite(parsed) ? parsed : 0;
  }

  addExtra(): void {
    if (!this.canAddExtra) {
      return;
    }

    this.createForm = {
      ...this.createForm,
      extras: [
        ...this.createForm.extras,
        {
          name: this.extraDraftName.trim(),
          priceDelta: this.extraDraftPrice
        }
      ]
    };

    this.extraDraftName = '';
    this.extraDraftPrice = 0;
  }

  removeExtra(index: number): void {
    this.createForm = {
      ...this.createForm,
      extras: this.createForm.extras.filter((_, i) => i !== index)
    };
  }

  saveProduct(): void {
    if (this.productModalMode === 'create') {
      this.createProduct();
      return;
    }

    this.updateProduct();
  }

  private createProduct(): void {
    if (this.isCreating || !this.canCreateProduct || this.createForm.price === null) {
      return;
    }

    const payload: CreateTenantProductPayload = {
      name: this.createForm.name.trim(),
      description: this.createForm.description.trim(),
      price: this.createForm.price,
      category: this.createForm.category,
      availableForOrder: this.createForm.availableForOrder,
      imageDataUrl: this.createImagePreview,
      variants: this.createForm.variants,
      extras: this.createForm.extras
    };

    this.isCreating = true;
    this.createErrorMessage = null;

    this.dashboardService.createTenantProduct(payload)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isCreating = false;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (created) => {
          // Aggiorno la UI immediatamente; la cache viene comunque rinfrescata al prossimo reload.
          const mapped = this.mapApiProduct(created);
          this.products = [mapped, ...this.products];
          this.closeProductModal(true);
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error creating tenant product:', err);
          // Non è un errore di caricamento lista: è errore creazione.
          if (err?.status === 400) {
            this.createErrorMessage = 'Dati non validi: controlla nome, categoria e prezzo.';
          } else if (err?.status === 401 || err?.status === 403) {
            this.createErrorMessage = 'Sessione scaduta o permessi insufficienti: rifai login e riprova.';
          } else if (err?.status === 413) {
            this.createErrorMessage = 'Immagine troppo grande: prova con una foto più piccola.';
          } else {
            this.createErrorMessage = 'Errore durante la creazione del prodotto.';
          }
        }
      });
  }

  private updateProduct(): void {
    if (this.isCreating || !this.canCreateProduct || this.createForm.price === null || this.editingProductId === null) {
      return;
    }

    const payload: UpdateTenantProductPayload = {
      name: this.createForm.name.trim(),
      description: this.createForm.description.trim(),
      price: this.createForm.price,
      category: this.createForm.category,
      availableForOrder: this.createForm.availableForOrder,
      imageDataUrl: this.createImagePreview
    };

    if (this.hasLoadedProductDetails || this.createForm.variants.length > 0 || this.createForm.extras.length > 0) {
      payload.variants = this.createForm.variants;
      payload.extras = this.createForm.extras;
    }

    this.isCreating = true;
    this.createErrorMessage = null;

    this.dashboardService.updateTenantProduct(this.editingProductId, payload)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isCreating = false;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (updated) => {
          const mapped = this.mapApiProduct(updated);
          this.products = this.products.map((p) => (p.id === mapped.id ? mapped : p));
          this.closeProductModal(true);
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error updating tenant product:', err);
          if (err?.status === 400) {
            this.createErrorMessage = 'Dati non validi: controlla nome, categoria e prezzo.';
          } else if (err?.status === 401 || err?.status === 403) {
            this.createErrorMessage = 'Sessione scaduta o permessi insufficienti: rifai login e riprova.';
          } else if (err?.status === 413) {
            this.createErrorMessage = 'Immagine troppo grande: prova con una foto più piccola.';
          } else {
            this.createErrorMessage = 'Errore durante il salvataggio del prodotto.';
          }
        }
      });
  }

  confirmDeleteProduct(): void {
    if (this.isDeleting || this.productToDelete == null) {
      return;
    }

    this.isDeleting = true;
    this.deleteErrorMessage = null;

    const id = this.productToDelete.id;

    this.dashboardService.disableTenantProduct(id)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isDeleting = false;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: () => {
          this.products = this.products.filter((p) => p.id !== id);
          this.productToDelete = null;
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          console.error('Error deleting tenant product:', err);
          if (typeof err === 'object' && err !== null && 'status' in err && ((err as any).status === 401 || (err as any).status === 403)) {
            this.deleteErrorMessage = 'Sessione scaduta o permessi insufficienti: rifai login e riprova.';
          } else {
            this.deleteErrorMessage = 'Errore durante l\'eliminazione del prodotto.';
          }
          this.cdr.markForCheck();
        }
      });
  }

  trackProduct(index: number, product: { id: number }): number {
    return product.id;
  }

  private buildEmptyCreateForm(): CreateProductForm {
    return {
      name: '',
      description: '',
      price: null,
      category: '',
      availableForOrder: true,
      variants: [],
      extras: []
    };
  }

  private setPickedImage(file: File): void {
    if (!file.type.startsWith('image/')) {
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (typeof result === 'string') {
        this.createImagePreview = result;
      }
    };
    reader.readAsDataURL(file);
  }

  private loadProducts(forceRefresh = false): void {
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();
    const products$ = forceRefresh
      ? this.dashboardService.refreshTenantProducts()
      : this.dashboardService.getTenantProducts();

    products$
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (products: TenantProduct[]) => {
          this.products = Array.isArray(products)
            ? products.map((product) => this.mapApiProduct(product))
            : [];
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          console.error('Error loading tenant products:', err);
          this.products = [];
          this.hasError = true;
          this.cdr.markForCheck();
        }
      });
  }

  private mapApiProduct(product: TenantProduct): MenuProduct {
    const tone = this.tones[product.id % this.tones.length];
    return {
      id: product.id,
      name: product.name,
      description: product.description || 'Prodotto del menu tenant',
      price: product.price,
      category: product.category,
      imageLabel: (product.name || 'P').charAt(0).toUpperCase(),
      imageTone: tone,
      status: product.availableForOrder ? 'available' : 'unavailable',
      variants: product.variantsCount || 0,
      extras: product.extrasCount || 0,
      department: product.department,
      vatRate: product.vatRate,
      availableForOrder: product.availableForOrder,
      sku: product.sku,
      imageUrl: product.imageUrl || undefined
    };
  }
}

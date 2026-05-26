import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs/operators';
import { CategoryService, TenantCategory, CreateCategoryRequest, UpdateCategoryRequest } from '../../services/category.service';

interface CategoryEditForm {
  id?: number;
  name: string;
  description: string;
  displayOrder: number;
  status: 'ACTIVE' | 'DISABLED';
}

@Component({
  selector: 'app-categories-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="categories-panel">
      <div class="panel-header">
        <h3>Gestione Categorie</h3>
        <button type="button" class="btn" (click)="loadCategories()" [disabled]="isLoading || isSaving">
          {{ isLoading ? 'Caricamento…' : 'Ricarica' }}
        </button>
      </div>

      <p class="hint" *ngIf="hasError">Errore nel caricamento/salvataggio delle categorie.</p>
      <p class="hint success" *ngIf="!hasError && lastSavedAt">Salvato: {{ lastSavedAt | date: 'dd/MM/yyyy HH:mm' }}</p>

      <!-- Form per aggiungere/modificare categoria -->
      <div class="form-section">
        <h4>{{ editingId ? 'Modifica Categoria' : 'Nuova Categoria' }}</h4>
        
        <div class="form-grid">
          <div class="field">
            <label>Nome *</label>
            <input type="text" [(ngModel)]="editForm.name" placeholder="Nome della categoria" />
          </div>

          <div class="field">
            <label>Ordine visualizzazione</label>
            <input type="number" [(ngModel)]="editForm.displayOrder" placeholder="0" />
          </div>

          <div class="field full">
            <label>Descrizione</label>
            <textarea [(ngModel)]="editForm.description" placeholder="Descrizione facoltativa" rows="2"></textarea>
          </div>

          <div class="field full" *ngIf="editingId">
            <label class="checkbox">
              <input type="checkbox" [checked]="editForm.status === 'ACTIVE'" 
                     (change)="editForm.status = editForm.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'" />
              <span>Attivo</span>
            </label>
          </div>

          <div class="field full">
            <button type="button" class="btn primary" (click)="save()" [disabled]="!editForm.name.trim() || isSaving">
              {{ isSaving ? 'Salvataggio…' : (editingId ? 'Aggiorna' : 'Crea') }}
            </button>
            <button type="button" class="btn" (click)="resetForm()" *ngIf="editingId">
              Annulla
            </button>
          </div>
        </div>
      </div>

      <!-- Lista categorie -->
      <div class="list-section">
        <h4>Categorie Esistenti</h4>
        
        <div class="loading" *ngIf="isLoading">Caricamento categorie…</div>
        
        <div class="empty" *ngIf="!isLoading && categories.length === 0">
          Nessuna categoria creata
        </div>

        <div class="categories-list" *ngIf="!isLoading && categories.length > 0">
          <div class="category-item" *ngFor="let cat of categories; let i = index" [class.disabled]="cat.status === 'DISABLED'">
            <div class="category-info">
              <div class="category-name">{{ cat.name }}</div>
              <div class="category-meta">
                <span class="order">Ordine: {{ cat.displayOrder }}</span>
                <span class="status" [class.active]="cat.status === 'ACTIVE'">{{ cat.status === 'ACTIVE' ? 'Attivo' : 'Disattivato' }}</span>
              </div>
              <div class="category-description" *ngIf="cat.description">{{ cat.description }}</div>
            </div>
            <div class="category-actions">
              <button type="button" class="btn small" (click)="edit(cat)" [disabled]="isSaving">
                Modifica
              </button>
              <button type="button" class="btn small danger" (click)="confirmDelete(cat)" [disabled]="isSaving">
                Elimina
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Conferma eliminazione -->
      <div class="modal" *ngIf="deleteConfirm">
        <div class="modal-content">
          <h4>Conferma eliminazione</h4>
          <p>Sei sicuro di voler eliminare la categoria "<strong>{{ deleteConfirm.name }}</strong>"?</p>
          <div class="modal-actions">
            <button type="button" class="btn" (click)="deleteConfirm = null">Annulla</button>
            <button type="button" class="btn danger" (click)="delete()" [disabled]="isSaving">
              {{ isSaving ? 'Eliminazione…' : 'Elimina' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
    .categories-panel {
      border: 1px solid #eef2f7;
      border-radius: 10px;
      padding: 14px;
      margin-top: 12px;
      background: #fbfdff;
    }
    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 10px;
    }
    .panel-header h3 {
      margin: 0;
      font-size: 1.1rem;
    }
    .form-section, .list-section {
      margin-top: 16px;
      padding: 12px;
      background: white;
      border-radius: 8px;
      border: 1px solid #e5e7eb;
    }
    .form-section h4, .list-section h4 {
      margin: 0 0 12px 0;
      font-size: 0.95rem;
      color: #374151;
    }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
    }
    .field {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .field.full {
      grid-column: 1 / -1;
    }
    label {
      font-size: 0.85rem;
      color: #111827;
      font-weight: 500;
    }
    input[type="text"], input[type="number"], textarea {
      border: 1px solid #d1d5db;
      border-radius: 6px;
      padding: 8px 10px;
      font-family: inherit;
      font-size: 0.9rem;
    }
    textarea {
      resize: vertical;
    }
    .checkbox {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.9rem;
    }
    .btn {
      border: 1px solid #d1d5db;
      background: #fff;
      border-radius: 6px;
      padding: 8px 12px;
      cursor: pointer;
      font-size: 0.85rem;
      transition: all 0.2s;
    }
    .btn:hover:not([disabled]) {
      background: #f3f4f6;
      border-color: #9ca3af;
    }
    .btn.primary {
      background: #111827;
      color: #fff;
      border-color: #111827;
    }
    .btn.primary:hover:not([disabled]) {
      background: #1f2937;
    }
    .btn.small {
      padding: 6px 10px;
      font-size: 0.8rem;
    }
    .btn.danger {
      color: #dc2626;
      border-color: #fca5a5;
    }
    .btn.danger:hover:not([disabled]) {
      background: #fee2e2;
    }
    .btn[disabled] {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .hint {
      color: #6b7280;
      font-size: 0.85rem;
      margin: 8px 0;
    }
    .hint.success {
      color: #065f46;
    }
    .loading, .empty {
      padding: 16px;
      text-align: center;
      color: #6b7280;
      font-size: 0.9rem;
    }
    .categories-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .category-item {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      padding: 10px;
      border: 1px solid #e5e7eb;
      border-radius: 6px;
      background: #f9fafb;
    }
    .category-item.disabled {
      opacity: 0.7;
      background: #f3f4f6;
    }
    .category-info {
      flex: 1;
    }
    .category-name {
      font-weight: 600;
      color: #111827;
      margin-bottom: 4px;
    }
    .category-meta {
      display: flex;
      gap: 12px;
      margin-bottom: 4px;
    }
    .category-meta span {
      font-size: 0.8rem;
      color: #6b7280;
    }
    .category-meta .status {
      padding: 2px 6px;
      border-radius: 4px;
      background: #fee2e2;
      color: #dc2626;
    }
    .category-meta .status.active {
      background: #dcfce7;
      color: #065f46;
    }
    .category-description {
      font-size: 0.85rem;
      color: #6b7280;
      margin-top: 4px;
    }
    .category-actions {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
    .modal {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-content {
      background: white;
      border-radius: 8px;
      padding: 20px;
      max-width: 400px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    }
    .modal-content h4 {
      margin: 0 0 10px 0;
    }
    .modal-content p {
      margin: 0 0 16px 0;
      color: #374151;
    }
    .modal-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
    }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoriesManagerComponent implements OnInit {
  private readonly categoryService: CategoryService = inject(CategoryService);
  private readonly cdr = inject(ChangeDetectorRef);

  categories: TenantCategory[] = [];
  editForm: CategoryEditForm = this.createEmptyForm();
  editingId: number | null = null;
  deleteConfirm: TenantCategory | null = null;

  isLoading = false;
  isSaving = false;
  hasError = false;
  lastSavedAt: Date | null = null;

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    this.categoryService
      .getCategories()
      .pipe(
        timeout(8000),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (categories: TenantCategory[]) => {
          this.categories = categories.sort((a: TenantCategory, b: TenantCategory) => a.displayOrder - b.displayOrder);
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.categories = [];
          this.cdr.markForCheck();
        }
      });
  }

  edit(category: TenantCategory): void {
    this.editingId = category.id;
    this.editForm = {
      id: category.id,
      name: category.name,
      description: category.description,
      displayOrder: category.displayOrder,
      status: category.status
    };
    this.cdr.markForCheck();
  }

  resetForm(): void {
    this.editingId = null;
    this.editForm = this.createEmptyForm();
    this.deleteConfirm = null;
    this.cdr.markForCheck();
  }

  save(): void {
    if (!this.editForm.name.trim()) {
      return;
    }

    this.isSaving = true;
    this.hasError = false;
    this.cdr.markForCheck();

    if (this.editingId !== null) {
      // Update
      const request: UpdateCategoryRequest = {
        name: this.editForm.name,
        description: this.editForm.description,
        displayOrder: this.editForm.displayOrder,
        status: this.editForm.status
      };

      const editingId = this.editingId;
      this.categoryService
        .updateCategory(editingId, request)
        .pipe(
          timeout(8000),
          finalize(() => {
            this.isSaving = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe({
          next: () => {
            this.lastSavedAt = new Date();
            this.resetForm();
            this.loadCategories();
          },
          error: () => {
            this.hasError = true;
            this.cdr.markForCheck();
          }
        });
    } else {
      // Create
      const request: CreateCategoryRequest = {
        name: this.editForm.name,
        description: this.editForm.description,
        displayOrder: this.editForm.displayOrder
      };

      this.categoryService
        .createCategory(request)
        .pipe(
          timeout(8000),
          finalize(() => {
            this.isSaving = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe({
          next: () => {
            this.lastSavedAt = new Date();
            this.resetForm();
            this.loadCategories();
          },
          error: () => {
            this.hasError = true;
            this.cdr.markForCheck();
          }
        });
    }
  }

  confirmDelete(category: TenantCategory): void {
    this.deleteConfirm = category;
    this.cdr.markForCheck();
  }

  delete(): void {
    if (!this.deleteConfirm) {
      return;
    }

    this.isSaving = true;
    this.hasError = false;
    this.cdr.markForCheck();

    const categoryId = this.deleteConfirm.id;
    this.categoryService
      .deleteCategory(categoryId)
      .pipe(
        timeout(8000),
        finalize(() => {
          this.isSaving = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.lastSavedAt = new Date();
          this.deleteConfirm = null;
          this.loadCategories();
        },
        error: () => {
          this.hasError = true;
          this.cdr.markForCheck();
        }
      });
  }

  private createEmptyForm(): CategoryEditForm {
    return {
      name: '',
      description: '',
      displayOrder: 0,
      status: 'ACTIVE'
    };
  }
}

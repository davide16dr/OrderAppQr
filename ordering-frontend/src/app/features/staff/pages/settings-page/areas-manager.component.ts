import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs/operators';
import { DashboardService, TenantArea } from '../../services/dashboard.service';

interface AreaEditForm {
  name: string;
  displayOrder: number;
  status: 'ACTIVE' | 'DISABLED';
}

@Component({
  selector: 'app-areas-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="areas-panel">
      <div class="panel-header">
        <h3>Gestione Aree Postazioni</h3>
        <button type="button" class="btn" (click)="loadAreas()" [disabled]="isLoading || isSaving">
          {{ isLoading ? 'Caricamento…' : 'Ricarica' }}
        </button>
      </div>

      <p class="hint" *ngIf="hasError">Errore nel caricamento/salvataggio delle aree.</p>
      <p class="hint success" *ngIf="!hasError && lastSavedAt">Salvato: {{ lastSavedAt | date: 'dd/MM/yyyy HH:mm' }}</p>

      <div class="form-section">
        <h4>{{ editingId ? 'Modifica Area' : 'Nuova Area' }}</h4>

        <div class="form-grid">
          <div class="field">
            <label>Nome *</label>
            <input type="text" [(ngModel)]="editForm.name" placeholder="Nome area (es: Zona Spiaggia)" />
          </div>

          <div class="field">
            <label>Ordine visualizzazione</label>
            <input type="number" [(ngModel)]="editForm.displayOrder" placeholder="0" />
          </div>

          <div class="field full" *ngIf="editingId">
            <label class="checkbox">
              <input
                type="checkbox"
                [checked]="editForm.status === 'ACTIVE'"
                (change)="editForm.status = editForm.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'"
              />
              <span>Attiva</span>
            </label>
          </div>

          <div class="field full actions">
            <button type="button" class="btn primary" (click)="save()" [disabled]="!editForm.name.trim() || isSaving">
              {{ isSaving ? 'Salvataggio…' : (editingId ? 'Aggiorna' : 'Crea') }}
            </button>
            <button type="button" class="btn" (click)="resetForm()" *ngIf="editingId">Annulla</button>
          </div>
        </div>
      </div>

      <div class="list-section">
        <h4>Aree Esistenti</h4>

        <div class="loading" *ngIf="isLoading">Caricamento aree…</div>

        <div class="empty" *ngIf="!isLoading && areas.length === 0">
          Nessuna area creata
        </div>

        <div class="areas-list" *ngIf="!isLoading && areas.length > 0">
          <div class="area-item" *ngFor="let area of areas" [class.disabled]="area.status === 'DISABLED'">
            <div class="area-info">
              <div class="area-name">{{ area.name }}</div>
              <div class="area-meta">
                <span>Ordine: {{ area.displayOrder }}</span>
                <span class="status" [class.active]="area.status === 'ACTIVE'">
                  {{ area.status === 'ACTIVE' ? 'Attiva' : 'Disattivata' }}
                </span>
              </div>
            </div>

            <div class="area-actions">
              <button type="button" class="btn small" (click)="edit(area)" [disabled]="isSaving">Modifica</button>
              <button type="button" class="btn small danger" (click)="confirmDelete(area)" [disabled]="isSaving">Elimina</button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal" *ngIf="deleteConfirm">
        <div class="modal-content">
          <h4>Conferma eliminazione</h4>
          <p>Sei sicuro di voler eliminare l'area "<strong>{{ deleteConfirm.name }}</strong>"?</p>
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
    .areas-panel {
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
      background: #fff;
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
    .field.actions {
      flex-direction: row;
      align-items: center;
    }
    label {
      font-size: 0.85rem;
      color: #111827;
      font-weight: 500;
    }
    input[type='text'], input[type='number'] {
      border: 1px solid #d1d5db;
      border-radius: 6px;
      padding: 8px 10px;
      font-family: inherit;
      font-size: 0.9rem;
      width: 100%;
      box-sizing: border-box;
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
    .btn.small {
      padding: 6px 10px;
      font-size: 0.8rem;
    }
    .btn.danger {
      color: #dc2626;
      border-color: #fca5a5;
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
    .areas-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .area-item {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      padding: 10px;
      border: 1px solid #e5e7eb;
      border-radius: 6px;
      background: #f9fafb;
    }
    .area-item.disabled {
      opacity: 0.7;
      background: #f3f4f6;
    }
    .area-info {
      flex: 1;
    }
    .area-name {
      font-weight: 600;
      color: #111827;
      margin-bottom: 4px;
    }
    .area-meta {
      display: flex;
      gap: 12px;
    }
    .area-meta span {
      font-size: 0.8rem;
      color: #6b7280;
    }
    .area-meta .status {
      padding: 2px 6px;
      border-radius: 4px;
      background: #fee2e2;
      color: #dc2626;
    }
    .area-meta .status.active {
      background: #dcfce7;
      color: #065f46;
    }
    .area-actions {
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
      background: #fff;
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

    @media (max-width: 520px) {
      .areas-panel { padding: 10px; }
      .form-grid { grid-template-columns: 1fr; }
      .field.full { grid-column: auto; }
      .area-item { flex-direction: column; align-items: stretch; gap: 8px; }
      .area-actions { justify-content: flex-start; }
      .modal-content { padding: 16px; margin: 0 12px; }
    }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AreasManagerComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  areas: TenantArea[] = [];
  editForm: AreaEditForm = this.createEmptyForm();
  editingId: number | null = null;
  deleteConfirm: TenantArea | null = null;

  isLoading = false;
  isSaving = false;
  hasError = false;
  lastSavedAt: Date | null = null;

  ngOnInit(): void {
    this.loadAreas();
  }

  loadAreas(): void {
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    this.dashboardService
      .refreshTenantAreas()
      .pipe(
        timeout(8000),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (areas) => {
          this.areas = (Array.isArray(areas) ? areas : []).sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name));
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.areas = [];
          this.cdr.markForCheck();
        }
      });
  }

  edit(area: TenantArea): void {
    this.editingId = area.id;
    this.editForm = {
      name: area.name,
      displayOrder: area.displayOrder,
      status: area.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE'
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
      this.dashboardService
        .updateTenantArea(this.editingId, {
          name: this.editForm.name.trim(),
          displayOrder: this.editForm.displayOrder,
          status: this.editForm.status
        })
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
            this.loadAreas();
          },
          error: () => {
            this.hasError = true;
            this.cdr.markForCheck();
          }
        });
      return;
    }

    this.dashboardService
      .createTenantArea(this.editForm.name.trim())
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
          this.loadAreas();
        },
        error: () => {
          this.hasError = true;
          this.cdr.markForCheck();
        }
      });
  }

  confirmDelete(area: TenantArea): void {
    this.deleteConfirm = area;
    this.cdr.markForCheck();
  }

  delete(): void {
    if (!this.deleteConfirm) {
      return;
    }

    this.isSaving = true;
    this.hasError = false;
    this.cdr.markForCheck();

    const areaId = this.deleteConfirm.id;
    this.dashboardService
      .disableTenantArea(areaId)
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
          this.loadAreas();
        },
        error: () => {
          this.hasError = true;
          this.cdr.markForCheck();
        }
      });
  }

  private createEmptyForm(): AreaEditForm {
    return {
      name: '',
      displayOrder: 0,
      status: 'ACTIVE'
    };
  }
}
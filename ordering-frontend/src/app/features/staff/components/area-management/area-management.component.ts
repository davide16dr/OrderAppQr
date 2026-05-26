import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { DashboardService, TenantArea } from '../../services/dashboard.service';
import { finalize, retry, timeout } from 'rxjs/operators';

@Component({
  selector: 'app-area-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './area-management.component.html',
  styleUrl: './area-management.component.scss'
})
export class AreaManagementComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  areas: TenantArea[] = [];
  isLoading = true;
  errorMessage: string | null = null;

  newAreaName = '';
  isSaving = false;
  deletingAreaIds = new Set<number>();
  editingAreaId: number | null = null;
  editAreaName = '';
  editDisplayOrder = 0;
  editStatus: 'ACTIVE' | 'DISABLED' = 'ACTIVE';

  ngOnInit(): void {
    this.reloadAreas();
  }

  reloadAreas(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.dashboardService
      .refreshTenantAreas()
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoading = false;
        })
      )
      .subscribe({
        next: (areas) => {
          this.areas = (Array.isArray(areas) ? areas : []).sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name));
          if (this.editingAreaId !== null && !this.areas.some((a) => a.id === this.editingAreaId)) {
            this.cancelEdit();
          }
        },
        error: (err) => {
          console.error('Errore caricamento aree:', err);
          this.errorMessage = 'Errore nel caricamento delle aree.';
          this.areas = [];
        }
      });
  }

  canAdd(): boolean {
    return !this.isSaving && this.newAreaName.trim().length > 0;
  }

  addArea(): void {
    if (!this.canAdd()) {
      return;
    }

    const name = this.newAreaName.trim();
    this.isSaving = true;
    this.errorMessage = null;

    this.dashboardService
      .createTenantArea(name)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isSaving = false;
        })
      )
      .subscribe({
        next: () => {
          this.newAreaName = '';
          this.reloadAreas();
        },
        error: (err) => {
          console.error('Errore creazione area:', err);
          if (typeof err === 'object' && err !== null && 'status' in err && (err as any).status === 409) {
            this.errorMessage = 'Area già esistente.';
          } else {
            this.errorMessage = 'Errore durante la creazione.';
          }
        }
      });
  }

  startEdit(area: TenantArea): void {
    this.editingAreaId = area.id;
    this.editAreaName = area.name;
    this.editDisplayOrder = area.displayOrder;
    this.editStatus = area.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE';
    this.errorMessage = null;
  }

  cancelEdit(): void {
    this.editingAreaId = null;
    this.editAreaName = '';
    this.editDisplayOrder = 0;
    this.editStatus = 'ACTIVE';
  }

  canSaveEdit(): boolean {
    return this.editingAreaId !== null && !this.isSaving && this.editAreaName.trim().length > 0;
  }

  saveEdit(): void {
    if (!this.canSaveEdit() || this.editingAreaId === null) {
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    this.dashboardService
      .updateTenantArea(this.editingAreaId, {
        name: this.editAreaName.trim(),
        displayOrder: this.editDisplayOrder,
        status: this.editStatus
      })
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isSaving = false;
        })
      )
      .subscribe({
        next: () => {
          this.cancelEdit();
          this.reloadAreas();
        },
        error: (err: unknown) => {
          console.error('Errore aggiornamento area:', err);
          if (typeof err === 'object' && err !== null && 'status' in err && (err as any).status === 409) {
            this.errorMessage = 'Area già esistente.';
          } else {
            this.errorMessage = 'Errore durante l\'aggiornamento.';
          }
        }
      });
  }

  isDeleting(areaId: number): boolean {
    return this.deletingAreaIds.has(areaId);
  }

  removeArea(area: TenantArea): void {
    if (this.isDeleting(area.id)) {
      return;
    }

    this.errorMessage = null;
    this.deletingAreaIds.add(area.id);
    this.dashboardService
      .disableTenantArea(area.id)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.deletingAreaIds.delete(area.id);
        })
      )
      .subscribe({
        next: () => {
          this.reloadAreas();
        },
        error: (err) => {
          console.error('Errore rimozione area:', err);
          this.errorMessage = 'Errore durante la rimozione.';
        }
      });
  }
}

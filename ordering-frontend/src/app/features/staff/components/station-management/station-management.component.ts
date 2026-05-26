import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DashboardService, TenantArea, TenantStationSummary, TenantStationStats } from '../../services/dashboard.service';
import { StationCreateOverlayComponent } from './station-create-overlay/station-create-overlay.component';
import { StationCreatePayload } from './station-create-overlay/station-create-overlay.types';
import { finalize, retry, timeout } from 'rxjs/operators';

@Component({
  selector: 'app-station-management',
  standalone: true,
  imports: [CommonModule, FormsModule, StationCreateOverlayComponent],
  templateUrl: './station-management.component.html',
  styleUrl: './station-management.component.scss'
})
export class StationManagementComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  stations: TenantStationSummary[] = [];
  areas: TenantArea[] = [];
  stats: TenantStationStats | null = null;
  isLoading = true;
  errorMessage: string | null = null;
  createErrorMessage: string | null = null;
  isCreateFormOpen = false;
  isCreating = false;
  actionsOpenStationId: number | null = null;
  isUpdatingStation = false;
  isDeletingStation = false;

  filterName = '';
  filterAreaId: number | null = null;
  filterActive: boolean | null = null;

  ngOnInit(): void {
    this.reloadAreas();
    this.reloadStations();
    this.reloadStats();
  }

  openCreateForm(): void {
    this.createErrorMessage = null;
    this.isCreateFormOpen = true;
    this.cdr.markForCheck();
  }

  closeCreateForm(): void {
    this.isCreateFormOpen = false;
    this.createErrorMessage = null;
    this.cdr.markForCheck();
  }

  closeStationActions(): void {
    this.actionsOpenStationId = null;
    this.cdr.markForCheck();
  }

  private reloadAreas(): void {
    this.dashboardService
      .refreshTenantAreas()
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 })
      )
      .subscribe({
        next: (areas) => {
          this.areas = Array.isArray(areas) ? areas : [];
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Errore caricamento aree:', err);
          this.areas = [];
          this.cdr.markForCheck();
        }
      });
  }

  reloadStations(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.markForCheck();

    this.dashboardService
      .getTenantStations({
        name: this.filterName || undefined,
        areaId: this.filterAreaId || undefined,
        active: this.filterActive !== null ? this.filterActive : undefined
      })
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoading = false;
        })
      )
      .subscribe({
        next: (stations) => {
          this.stations = Array.isArray(stations) ? stations : [];
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Errore caricamento postazioni:', err);
          this.errorMessage = 'Errore nel caricamento delle postazioni.';
          this.stations = [];
          this.cdr.markForCheck();
        }
      });
  }

  createStation(payload: StationCreatePayload): void {
    if (this.isCreating) {
      return;
    }

    this.isCreating = true;
    this.cdr.markForCheck();

    this.dashboardService
      .createTenantStation({
        name: payload.name,
        type: payload.type,
        areaId: payload.areaId,
        capacity: payload.capacity,
        status: payload.status,
        notes: payload.notes,
        active: payload.active,
        generateQrAutomatically: payload.generateQrAutomatically
      })
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isCreating = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.closeCreateForm();
          this.reloadStations();
          this.reloadStats();
        },
        error: (err) => {
          console.error('Errore creazione postazione:', err);
          this.createErrorMessage = err?.error?.message || 'Errore durante la creazione della postazione.';
          this.cdr.markForCheck();
        }
      });
  }

  reloadStats(): void {
    this.dashboardService
      .getTenantStationStats()
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 })
      )
      .subscribe({
        next: (result) => {
          this.stats = result;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Errore caricamento statistiche:', err);
          this.stats = null;
          this.cdr.markForCheck();
        }
      });
  }

  onFilterChanged(): void {
    this.reloadStations();
  }

  openStationDetails(station: TenantStationSummary): void {
    this.actionsOpenStationId = this.actionsOpenStationId === station.id ? null : station.id;
    this.cdr.markForCheck();
  }

  toggleCustomerOrdering(station: TenantStationSummary): void {
    if (this.isUpdatingStation || this.isDeletingStation) {
      return;
    }

    this.isUpdatingStation = true;
    this.dashboardService
      .updateTenantStation(station.id, {
        name: station.name,
        type: station.type,
        areaId: station.areaId ?? undefined,
        capacity: station.capacity ?? undefined,
        status: station.status,
        notes: station.notes ?? undefined,
        active: !station.active
      })
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isUpdatingStation = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.actionsOpenStationId = null;
          this.reloadStations();
          this.reloadStats();
        },
        error: (err) => {
          console.error('Errore aggiornamento postazione:', err);
          this.errorMessage = 'Errore durante l\'aggiornamento della postazione.';
          this.cdr.markForCheck();
        }
      });
  }

  confirmDeleteStation(station: TenantStationSummary): void {
    if (this.isUpdatingStation || this.isDeletingStation) {
      return;
    }

    const confirmed = window.confirm(`Eliminare la postazione "${station.name}"? L\'operazione non può essere annullata.`);
    if (!confirmed) {
      return;
    }

    this.isDeletingStation = true;
    this.dashboardService
      .deleteTenantStation(station.id)
      .pipe(
        timeout(12000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isDeletingStation = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.actionsOpenStationId = null;
          this.reloadStations();
          this.reloadStats();
        },
        error: (err) => {
          console.error('Errore eliminazione postazione:', err);
          this.errorMessage = err?.error?.message || 'Errore durante l\'eliminazione della postazione.';
          this.cdr.markForCheck();
        }
      });
  }

  downloadQr(station: TenantStationSummary): void {
    this.dashboardService.downloadTenantStationQr(station.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `station-${station.id}-qr.png`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Errore download QR:', err);
        this.errorMessage = 'Errore durante il download del QR code.';
      }
    });
  }

  downloadAllQrs(): void {
    this.dashboardService.downloadAllTenantStationsQr().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `stations-qr.zip`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Errore download ZIP QR:', err);
        this.errorMessage = 'Errore durante il download dei QR.';
      }
    });
  }

  trackArea(index: number, area: TenantArea): number {
    return area.id;
  }

  isStationMenuOpen(station: TenantStationSummary): boolean {
    return this.actionsOpenStationId === station.id;
  }
}

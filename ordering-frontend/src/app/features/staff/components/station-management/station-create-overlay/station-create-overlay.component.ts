import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TenantArea } from '../../../services/dashboard.service';
import {
  StationCreatePayload,
  StationTypeOption,
} from './station-create-overlay.types';

@Component({
  selector: 'app-station-create-overlay',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './station-create-overlay.component.html',
  styleUrl: './station-create-overlay.component.scss'
})
export class StationCreateOverlayComponent {
  private _areas: TenantArea[] = [];

  @Input()
  set areas(value: TenantArea[]) {
    this._areas = Array.isArray(value) ? value : [];
    this.syncDefaultArea();
  }

  get areas(): TenantArea[] {
    return this._areas;
  }

  @Input() errorMessage: string | null = null;
  @Input() isSubmitting = false;

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<StationCreatePayload>();

  readonly stationTypeOptions: StationTypeOption[] = [
    { value: 'TABLE', label: 'Tavolo' },
    { value: 'UMBRELLA', label: 'Ombrellone' },
    { value: 'SUNBED', label: 'Lettino' },
    { value: 'LOUNGE', label: 'Lounge' },
    { value: 'VIP_AREA', label: 'Area VIP' },
    { value: 'SERVICE_POINT', label: 'Postazione servizio' }
  ];

  model: StationCreatePayload = {
    name: '',
    type: 'UMBRELLA',
    areaId: 0,
    capacity: 1,
    status: 'AVAILABLE',
    notes: '',
    active: true,
    generateQrAutomatically: true
  };

  localErrorMessage: string | null = null;

  constructor() {
    queueMicrotask(() => this.syncDefaultArea());
  }

  stopPropagation(event: MouseEvent): void {
    event.stopPropagation();
  }

  onBackdropClick(): void {
    this.close.emit();
  }

  canSubmit(): boolean {
    return this.model.name.trim().length > 0 && this.model.areaId > 0 && this.model.capacity > 0;
  }

  submitForm(): void {
    this.localErrorMessage = null;

    if (!this.canSubmit()) {
      this.localErrorMessage = 'Compila nome, area e capacità prima di salvare.';
      return;
    }

    this.save.emit({
      ...this.model,
      name: this.model.name.trim(),
      notes: this.model.notes.trim()
    });
  }

  resetForm(): void {
    this.model = {
      name: '',
      type: 'UMBRELLA',
      areaId: this.areas[0]?.id ?? 0,
      capacity: 1,
      status: 'AVAILABLE',
      notes: '',
      active: true,
      generateQrAutomatically: true
    };
    this.localErrorMessage = null;
  }

  private syncDefaultArea(): void {
    if (this.model.areaId > 0) {
      return;
    }

    const firstArea = this.areas[0];
    if (firstArea) {
      this.model.areaId = firstArea.id;
    }
  }
}

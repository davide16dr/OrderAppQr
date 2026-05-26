import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StationManagementComponent } from '../../components/station-management/station-management.component';

@Component({
  selector: 'app-stations-page',
  standalone: true,
  imports: [CommonModule, StationManagementComponent],
  template: `<app-station-management></app-station-management>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StationsPageComponent {}

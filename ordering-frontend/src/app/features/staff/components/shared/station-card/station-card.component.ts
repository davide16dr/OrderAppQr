import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantStationSummary } from '../../../services/dashboard.service';

@Component({
  selector: 'app-station-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="station-card" *ngIf="station">
      <h4>{{ station.name }}</h4>
      <p>{{ station.areaName || 'Senza area' }}</p>
      <p>{{ station.status }} - {{ station.active ? 'Attiva' : 'Disattiva' }}</p>
    </article>
  `,
  styles: ['.station-card{background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:10px;} h4{margin:0 0 6px;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StationCardComponent {
  @Input() station: TenantStationSummary | null = null;
}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [ngClass]="tone">{{ label }}</span>`,
  styles: [`.badge{padding:4px 10px;border-radius:999px;font-size:12px;font-weight:700;display:inline-block;}
    .neutral{background:#e5e7eb;color:#374151}.success{background:#dcfce7;color:#166534}.warning{background:#fef3c7;color:#92400e}.danger{background:#fee2e2;color:#991b1b}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusBadgeComponent {
  @Input() label = 'Sconosciuto';
  @Input() tone: 'neutral' | 'success' | 'warning' | 'danger' = 'neutral';
}

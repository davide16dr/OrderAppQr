import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="empty-wrap">{{ message }}</div>`,
  styles: [`.empty-wrap{padding:20px;border:1px dashed #cbd5e1;border-radius:8px;color:#64748b;text-align:center;}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyStateComponent {
  @Input() message = 'Nessun dato disponibile.';
}

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="error-wrap">
      <p>{{ message }}</p>
      <button type="button" (click)="retry.emit()">Riprova</button>
    </div>
  `,
  styles: [
    `.error-wrap {border:1px solid #fecaca;background:#fef2f2;color:#991b1b;padding:12px;border-radius:8px;display:flex;justify-content:space-between;align-items:center;gap:12px;}
     button {border:1px solid #fca5a5;background:#fff;padding:6px 10px;border-radius:6px;cursor:pointer;}`
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErrorStateComponent {
  @Input() message = 'Errore durante il caricamento.';
  @Output() retry = new EventEmitter<void>();
}

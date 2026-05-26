import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="backdrop" *ngIf="open" (click)="cancel.emit()">
      <div class="panel" (click)="$event.stopPropagation()">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <div class="actions">
          <button type="button" (click)="cancel.emit()">Annulla</button>
          <button type="button" class="danger" (click)="confirm.emit()">Conferma</button>
        </div>
      </div>
    </div>
  `,
  styles: ['.backdrop{position:fixed;inset:0;background:rgba(17,24,39,.5);display:flex;align-items:center;justify-content:center;z-index:90;} .panel{background:#fff;padding:16px;border-radius:10px;max-width:420px;width:100%;} .actions{display:flex;justify-content:flex-end;gap:8px;} .danger{background:#dc2626;color:#fff;border:0;padding:8px 12px;border-radius:6px;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Conferma';
  @Input() message = 'Vuoi continuare?';
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}

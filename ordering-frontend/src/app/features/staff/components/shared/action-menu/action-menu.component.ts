import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-action-menu',
  standalone: true,
  imports: [CommonModule],
  template: '<div class="menu"><ng-content></ng-content></div>',
  styles: ['.menu{display:inline-flex;gap:6px;align-items:center;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ActionMenuComponent {}

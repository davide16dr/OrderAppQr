import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule],
  template: '<div class="filter-bar"><ng-content></ng-content></div>',
  styles: ['.filter-bar{display:flex;gap:10px;align-items:center;flex-wrap:wrap;}'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FilterBarComponent {}

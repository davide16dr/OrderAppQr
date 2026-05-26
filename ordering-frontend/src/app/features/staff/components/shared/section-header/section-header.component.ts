import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-section-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="section-head">
      <h2>{{ title }}</h2>
      <p *ngIf="subtitle">{{ subtitle }}</p>
    </div>
  `,
  styles: [`.section-head{margin-bottom:14px;}h2{margin:0;font-size:22px;}p{margin:4px 0 0;color:#6b7280;}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SectionHeaderComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle = '';
}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="loading-wrap">
      <div class="spinner"></div>
      <p>{{ message }}</p>
    </div>
  `,
  styles: [
    `.loading-wrap {display:flex;flex-direction:column;align-items:center;gap:10px;padding:28px;color:#4b5563;}
     .spinner {width:34px;height:34px;border:4px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;animation:spin 1s linear infinite;}
     @keyframes spin {from {transform:rotate(0deg);} to {transform:rotate(360deg);}}`
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoadingStateComponent {
  @Input() message = 'Caricamento in corso...';
}

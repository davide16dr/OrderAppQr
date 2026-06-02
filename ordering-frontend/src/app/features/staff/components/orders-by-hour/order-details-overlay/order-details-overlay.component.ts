import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { OrderStatus, StaffOrderCard } from '../../../models/staff-order.model';

@Component({
  selector: 'app-order-details-overlay',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (order) {
      <div class="overlay-backdrop" (click)="onBackdropClick()">
        <div class="overlay-panel" (click)="stopPropagation($event)">
          <div class="overlay-header">
            <div class="overlay-title-group">
              <div class="overlay-code">{{ order.code }}</div>
              <div class="overlay-status status-{{ order.status }}">{{ getStatusLabel(order.status) }}</div>
            </div>

            <button type="button" class="icon-button" (click)="close.emit()" aria-label="Chiudi dettaglio ordine">
              <span aria-hidden="true">×</span>
            </button>
          </div>

          <div class="overlay-body">
            <section class="summary-grid">
              <div class="summary-card">
                <div class="summary-label">Posizione</div>
                <div class="summary-value">{{ order.locationLabel }}</div>
                <div class="summary-subtitle">{{ order.areaName }}</div>
              </div>

              <div class="summary-card">
                <div class="summary-label">Orario</div>
                <div class="summary-value">{{ order.timeLabel || '—' }}</div>
                <div class="summary-subtitle">{{ order.dateLabel || '—' }}</div>
              </div>
            </section>

            <section class="items-section">
              <h3>Prodotti ordinati</h3>
              <div class="items-list">
                @for (line of order.items; track line.name) {
                  <div class="item-row">
                    <div class="item-texts">
                      <div class="item-name">{{ line.name }}</div>

                      @if (getModifierSections(line).length) {
                        <div class="item-details">
                          @for (section of getModifierSections(line); track section.label) {
                            <div class="details-row">
                              <span class="details-label">{{ section.label }}</span>
                              <div class="details-chips">
                                @for (option of section.options; track option) {
                                  <span class="details-chip">{{ option }}</span>
                                }
                              </div>
                            </div>
                          }
                        </div>
                      }
                    </div>

                    <div class="item-right-group">
                      <span class="item-quantity">{{ line.quantity }}x</span>
                      <div class="item-total">€{{ line.total | number: '1.2-2' }}</div>
                    </div>
                  </div>
                }
              </div>
            </section>

            @if (order.note) {
              <section class="note-section">
                <div class="note-label">Note del cliente</div>
                <div class="note-text">{{ order.note }}</div>
              </section>
            }

            <section class="total-section">
              <span>Totale ordine</span>
              <strong>€{{ order.total | number: '1.2-2' }}</strong>
            </section>
          </div>

          <div class="overlay-footer">
            <button type="button" class="secondary-button" (click)="onPrint()">
              Stampa
            </button>

            <div class="footer-spacer"></div>

            @if (canAdvance()) {
              <button type="button" class="primary-button" (click)="onAdvance()">
                {{ getNextActionLabel() }}
                <span aria-hidden="true">›</span>
              </button>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    :host {
      position: fixed;
      inset: 0;
      z-index: 70;
    }

    .overlay-backdrop {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      background: rgba(15, 23, 42, 0.52);
      backdrop-filter: blur(10px);
    }

    .overlay-panel {
      width: min(100%, 640px);
      max-height: min(90vh, 920px);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border-radius: 24px;
      background: #ffffff;
      box-shadow: 0 30px 80px rgba(15, 23, 42, 0.32);
      transform-origin: center;
      animation: pop-in 180ms ease-out;
    }

    .overlay-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      padding: 20px;
      border-bottom: 1px solid #eef2f7;
    }

    .overlay-title-group {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .overlay-code {
      padding: 10px 14px;
      border-radius: 14px;
      background: #f1f5f9;
      color: #334155;
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.02em;
    }

    .overlay-status {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 7px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 800;
    }

    .status-received {
      background: #dbeafe;
      color: #1d4ed8;
    }

    .status-delivered {
      background: #dcfce7;
      color: #15803d;
    }

    .icon-button {
      width: 40px;
      height: 40px;
      border: 0;
      border-radius: 12px;
      background: transparent;
      color: #64748b;
      font-size: 28px;
      line-height: 1;
      cursor: pointer;
      transition: background 160ms ease, color 160ms ease;
    }

    .icon-button:hover {
      background: #f8fafc;
      color: #0f172a;
    }

    .overlay-body {
      flex: 1;
      overflow: auto;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 18px;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    .summary-card {
      padding: 16px;
      border-radius: 18px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .summary-label,
    .note-label {
      margin-bottom: 6px;
      font-size: 12px;
      font-weight: 700;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .summary-value {
      font-size: 18px;
      font-weight: 800;
      color: #0f172a;
    }

    .summary-subtitle {
      margin-top: 4px;
      font-size: 13px;
      color: #64748b;
    }

    .items-section h3 {
      margin: 0 0 12px;
      font-size: 18px;
      color: #0f172a;
    }

    .items-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .item-row {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      padding: 14px;
      border-radius: 16px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .item-quantity {
      align-self: flex-start;
      padding: 5px 9px;
      border-radius: 10px;
      background: #ffffff;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
      font-size: 12px;
      font-weight: 800;
      color: #334155;
      text-align: center;
    }

    .item-texts {
      display: flex;
      flex-direction: column;
      gap: 5px;
      flex: 1;
    }

    .item-name {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
    }

    .item-variant {
      font-size: 13px;
      color: #64748b;
    }

    .item-details {
      display: flex;
      flex-direction: column;
      gap: 5px;
      margin-top: 6px;
      padding-top: 8px;
      border-top: 1px dashed #e2e8f0;
    }

    .details-row {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .details-label {
      font-size: 11px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #94a3b8;
      white-space: nowrap;
      min-width: 52px;
    }

    .details-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }

    .details-chip {
      padding: 3px 10px;
      border-radius: 999px;
      background: #eff6ff;
      color: #1d4ed8;
      font-size: 12px;
      font-weight: 700;
      border: 1px solid #bfdbfe;
    }

    .item-right-group {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 10px;
      min-width: 62px;
    }

    .item-total {
      font-size: 15px;
      font-weight: 800;
      color: #0f172a;
      white-space: nowrap;
    }

    .note-section {
      padding: 16px;
      border-radius: 18px;
      background: #fffbeb;
      border: 1px solid #fde68a;
    }

    .note-text {
      color: #92400e;
      font-size: 14px;
      font-weight: 600;
      line-height: 1.5;
    }

    .total-section {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 16px 18px;
      border-radius: 18px;
      background: #0f172a;
      color: #ffffff;
    }

    .total-section span {
      font-size: 14px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.82);
    }

    .total-section strong {
      font-size: 28px;
      line-height: 1;
    }

    .overlay-footer {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 18px 20px 20px;
      border-top: 1px solid #eef2f7;
      background: #ffffff;
    }

    .footer-spacer {
      flex: 1;
    }

    .secondary-button,
    .primary-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      border: 0;
      border-radius: 14px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 700;
      transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
    }

    .secondary-button {
      min-height: 44px;
      padding: 0 16px;
      background: #ffffff;
      color: #0f172a;
      border: 1px solid #dbe4f0;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
    }

    .primary-button {
      min-height: 46px;
      padding: 0 18px;
      background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
      color: #ffffff;
      box-shadow: 0 12px 26px rgba(22, 163, 74, 0.24);
    }

    .secondary-button:hover,
    .primary-button:hover {
      transform: translateY(-1px);
    }

    @keyframes pop-in {
      from {
        opacity: 0;
        transform: translateY(16px) scale(0.98);
      }

      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    @media (max-width: 640px) {
      .overlay-backdrop {
        padding: 12px;
      }

      .overlay-panel {
        max-height: 92vh;
        border-radius: 20px;
      }

      .summary-grid {
        grid-template-columns: 1fr;
      }

      .overlay-footer {
        flex-wrap: wrap;
      }

      .footer-spacer {
        display: none;
      }

      .primary-button {
        width: 100%;
      }
    }
  `]
})
export class OrderDetailsOverlayComponent {
  @Input({ required: true }) order: StaffOrderCard | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() advanceStatus = new EventEmitter<number>();

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    this.close.emit();
  }

  getStatusLabel(status: OrderStatus): string {
    if (status === 'received') {
      return 'Ricevuto';
    }

    return 'Consegnato';
  }

  getStatusTone(status: OrderStatus): string {
    if (status === 'received') {
      return 'status-received';
    }

    return 'status-delivered';
  }

  canAdvance(): boolean {
    return this.order?.status !== 'delivered';
  }

  getNextActionLabel(): string {
    if (!this.order) {
      return '';
    }

    if (this.order.status === 'received') {
      return 'Segna consegnato';
    }

    return '';
  }

  onBackdropClick(): void {
    this.close.emit();
  }

  stopPropagation(event: MouseEvent): void {
    event.stopPropagation();
  }

  onAdvance(): void {
    if (!this.order) {
      return;
    }

    this.advanceStatus.emit(this.order.id);
  }

  onPrint(): void {
    window.print();
  }

  getModifierSections(line: StaffOrderCard['items'][number]): Array<{ label: string; options: string[] }> {
    const raw = (line as any).variant as string | undefined;
    if (!raw || !raw.trim()) {
      return [];
    }

    const tokens = raw.split(',').map((s) => s.trim()).filter(Boolean);
    const map = new Map<string, string[]>();
    const order: string[] = [];
    let currentLabel = 'Varianti';

    const ensureLabel = (label: string): void => {
      if (!map.has(label)) {
        map.set(label, []);
        order.push(label);
      }
      currentLabel = label;
    };

    for (const token of tokens) {
      const lower = token.toLowerCase();
      if (lower.startsWith('varianti:')) {
        ensureLabel('Varianti');
        const value = token.slice(token.indexOf(':') + 1).trim();
        if (value) map.get('Varianti')!.push(value);
      } else if (lower.startsWith('extra:')) {
        ensureLabel('Extra');
        const value = token.slice(token.indexOf(':') + 1).trim();
        if (value) map.get('Extra')!.push(value);
      } else {
        ensureLabel(currentLabel);
        map.get(currentLabel)!.push(token);
      }
    }

    return order.map(label => ({ label, options: map.get(label)! })).filter(s => s.options.length > 0);
  }
}

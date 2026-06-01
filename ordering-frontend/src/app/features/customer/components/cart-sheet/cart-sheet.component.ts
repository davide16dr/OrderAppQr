import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CartLine, ModifierGroup, ModifierOption, formatEuroFromCents } from '../../models/customer.types';

@Component({
  selector: 'app-cart-sheet',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cart-sheet.component.html',
  styleUrl: './cart-sheet.component.scss',
})
export class CartSheetComponent {
  @Input({ required: true }) lines: CartLine[] = [];
  @Input({ required: true }) totalCents = 0;
  @Input() note = '';
  @Input() showBackdrop = true;

  @Output() close = new EventEmitter<void>();
  @Output() noteChange = new EventEmitter<string>();
  @Output() removeLine = new EventEmitter<string>();
  @Output() decrement = new EventEmitter<string>();
  @Output() increment = new EventEmitter<string>();
  @Output() submit = new EventEmitter<void>();

  @Output() toggleModifierOption = new EventEmitter<{ lineKey: string; groupId: number; optionId: number }>();

  euro(cents: number): string {
    return formatEuroFromCents(cents);
  }

  onNoteChange(value: string): void {
    this.noteChange.emit(value);
  }

  onNoteInput(event: Event): void {
    const target = event.target as HTMLTextAreaElement | null;
    this.onNoteChange(target?.value ?? '');
  }

  trackByProductId(_: number, line: CartLine): string {
    return line.lineKey;
  }

  trackByGroupId(_: number, group: ModifierGroup): number {
    return group.id;
  }

  trackByOptionId(_: number, opt: ModifierOption): number {
    return opt.id;
  }

  isOptionSelected(line: CartLine, optionId: number): boolean {
    return (line.selectedModifierOptionIds ?? []).includes(optionId);
  }

  optionControlType(group: ModifierGroup): 'radio' | 'checkbox' {
    return group.maxSelectable === 1 ? 'radio' : 'checkbox';
  }

  deltaLabel(priceDeltaCents: number): string {
    if (!priceDeltaCents) {
      return '';
    }
    const abs = Math.abs(priceDeltaCents);
    const formatted = formatEuroFromCents(abs);
    return priceDeltaCents > 0 ? `+${formatted}` : `-${formatted}`;
  }
}

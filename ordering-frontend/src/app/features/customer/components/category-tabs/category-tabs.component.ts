import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MenuCategory } from '../../models/customer.types';

@Component({
  selector: 'app-category-tabs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-tabs.component.html',
  styleUrl: './category-tabs.component.scss',
})
export class CategoryTabsComponent {
  @Input({ required: true }) categories: MenuCategory[] = [];
  @Input() selectedId: string | null = null;

  @Output() selectedIdChange = new EventEmitter<string>();

  select(id: string): void {
    this.selectedIdChange.emit(id);
  }

  trackById(_: number, c: MenuCategory): string {
    return c.id;
  }
}

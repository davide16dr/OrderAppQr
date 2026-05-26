import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { CustomerLocationContext } from '../../models/customer.types';

@Component({
  selector: 'app-location-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './location-header.component.html',
  styleUrl: './location-header.component.scss',
})
export class LocationHeaderComponent {
  @Input({ required: true }) context!: CustomerLocationContext;
}

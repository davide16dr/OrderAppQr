import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardMainComponent } from '../../components/dashboard-main/dashboard-main.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, DashboardMainComponent],
  template: `<app-dashboard-main></app-dashboard-main>`
})
export class DashboardPageComponent {}

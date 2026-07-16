import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import QRCode from 'qrcode';

const DEMO_MENU_URL = 'https://www.orderappqr.it/customer/menu?tenant=demo&token=6a1a33f8c1eeee0ca29fa7c70778ea7e';

@Component({
  selector: 'app-demo-landing',
  templateUrl: './demo-landing.component.html',
  styleUrls: ['./demo-landing.component.scss'],
  standalone: true,
  imports: [CommonModule],
})
export class DemoLandingComponent implements OnInit {
  qrDataUrl = signal<string | null>(null);
  readonly menuUrl = DEMO_MENU_URL;

  constructor(private router: Router) {}

  ngOnInit(): void {
    QRCode.toDataURL(DEMO_MENU_URL, {
      width: 280,
      margin: 2,
      color: { dark: '#111827', light: '#ffffff' },
      errorCorrectionLevel: 'M',
    }).then(url => this.qrDataUrl.set(url));
  }

  goToDashboard(): void {
    this.router.navigate(['/public/login'], { queryParams: { demo: 'true' } });
  }

  openMenu(): void {
    window.open(DEMO_MENU_URL, '_blank', 'noopener,noreferrer');
  }
}

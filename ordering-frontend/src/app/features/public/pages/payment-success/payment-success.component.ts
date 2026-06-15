import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="ps-page">
      <div class="ps-card">
        <div class="ps-icon">
          <svg width="56" height="56" viewBox="0 0 56 56" fill="none">
            <circle cx="28" cy="28" r="28" fill="#dcfce7"/>
            <path d="M16 28l9 9 15-15" stroke="#16a34a" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="ps-title">Pagamento completato!</h1>
        <p class="ps-subtitle">
          Il tuo abbonamento è attivo. Puoi ora accedere al pannello di gestione
          con le credenziali ricevute via email.
        </p>
        <a class="ps-btn" routerLink="/public/login">Accedi al pannello</a>
        <p class="ps-note">
          Non hai ricevuto l'email? Controlla la cartella spam oppure
          contatta il supporto.
        </p>
      </div>
    </div>
  `,
  styles: [`
    .ps-page {
      min-height: 100vh;
      background: #f0fdf4;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }
    .ps-card {
      background: #fff;
      border-radius: 24px;
      padding: 48px 40px;
      max-width: 480px;
      width: 100%;
      text-align: center;
      box-shadow: 0 4px 32px rgba(0,0,0,0.08);
    }
    .ps-icon { margin-bottom: 24px; }
    .ps-title {
      font-size: 1.6rem;
      font-weight: 800;
      color: #111827;
      margin: 0 0 12px;
    }
    .ps-subtitle {
      color: #6b7280;
      line-height: 1.6;
      margin: 0 0 32px;
      font-size: 1rem;
    }
    .ps-btn {
      display: inline-block;
      background: #2f6de0;
      color: #fff;
      font-weight: 800;
      font-size: 0.95rem;
      padding: 14px 32px;
      border-radius: 999px;
      text-decoration: none;
      transition: filter 0.15s ease;
    }
    .ps-btn:hover { filter: brightness(0.92); }
    .ps-note {
      margin-top: 24px;
      font-size: 0.82rem;
      color: #9ca3af;
      line-height: 1.5;
    }
  `]
})
export class PaymentSuccessComponent {}

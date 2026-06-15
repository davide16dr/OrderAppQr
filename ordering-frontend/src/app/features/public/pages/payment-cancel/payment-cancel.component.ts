import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-payment-cancel',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="pc-page">
      <div class="pc-card">
        <div class="pc-icon">
          <svg width="56" height="56" viewBox="0 0 56 56" fill="none">
            <circle cx="28" cy="28" r="28" fill="#fef3c7"/>
            <path d="M28 18v13M28 35v2" stroke="#d97706" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="pc-title">Pagamento annullato</h1>
        <p class="pc-subtitle">
          Hai annullato il pagamento. Il tuo account è stato creato ma non è ancora attivo.
          Puoi completare il pagamento in qualsiasi momento dalla pagina di login.
        </p>
        <a class="pc-btn" routerLink="/public/login">Vai al login</a>
        <a class="pc-link" routerLink="/public/business-signup">Ricomincia la registrazione</a>
      </div>
    </div>
  `,
  styles: [`
    .pc-page {
      min-height: 100vh;
      background: #fffbeb;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }
    .pc-card {
      background: #fff;
      border-radius: 24px;
      padding: 48px 40px;
      max-width: 480px;
      width: 100%;
      text-align: center;
      box-shadow: 0 4px 32px rgba(0,0,0,0.08);
    }
    .pc-icon { margin-bottom: 24px; }
    .pc-title {
      font-size: 1.6rem;
      font-weight: 800;
      color: #111827;
      margin: 0 0 12px;
    }
    .pc-subtitle {
      color: #6b7280;
      line-height: 1.6;
      margin: 0 0 32px;
      font-size: 1rem;
    }
    .pc-btn {
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
    .pc-btn:hover { filter: brightness(0.92); }
    .pc-link {
      display: block;
      margin-top: 16px;
      color: #6b7280;
      font-size: 0.88rem;
      text-decoration: underline;
    }
  `]
})
export class PaymentCancelComponent {}

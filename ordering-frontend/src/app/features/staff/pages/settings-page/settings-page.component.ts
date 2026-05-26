import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, retry, timeout } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import { DashboardService, TenantSettings } from '../../services/dashboard.service';
import { CategoriesManagerComponent } from './categories-manager.component';
import { AreasManagerComponent } from './areas-manager.component';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, FormsModule, CategoriesManagerComponent, AreasManagerComponent],
  template: `
    <section class="settings-card" *ngIf="auth.currentUser() as user">
      <header class="header">
        <div>
          <h2>Impostazioni</h2>
          <p class="subtitle">Configurazioni operative del locale.</p>
        </div>
        <button type="button" class="btn" (click)="auth.logout()">Logout</button>
      </header>

      <section class="panel">
        <h3>Account</h3>
        <div class="grid">
          <div><strong>Nome:</strong> {{ user.firstName }} {{ user.lastName }}</div>
          <div><strong>Email:</strong> {{ user.email }}</div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3>Password</h3>
          <div class="panel-actions">
            <button type="button" class="btn primary" (click)="changePassword()" [disabled]="isChangingPassword">
              {{ isChangingPassword ? 'Aggiornamento…' : 'Cambia password' }}
            </button>
          </div>
        </div>

        <p class="hint" *ngIf="passwordSuccess">Password aggiornata con successo.</p>
        <p class="hint" *ngIf="passwordError">{{ passwordError }}</p>

        <div class="form-grid">
          <div class="field">
            <label>Password attuale</label>
            <input type="password" [(ngModel)]="currentPassword" autocomplete="current-password" />
          </div>

          <div class="field">
            <label>Nuova password</label>
            <input type="password" [(ngModel)]="newPassword" autocomplete="new-password" />
          </div>

          <div class="field">
            <label>Conferma nuova password</label>
            <input type="password" [(ngModel)]="confirmNewPassword" autocomplete="new-password" />
          </div>

          <div class="field full">
            <p class="hint">Minimo 8 caratteri. Se hai ricevuto una password temporanea via email, cambiala qui.</p>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3>Orari e ordinazioni</h3>
          <div class="panel-actions">
            <button type="button" class="btn" (click)="loadSettings()" [disabled]="isLoading || isSaving">
              {{ isLoading ? 'Caricamento…' : 'Ricarica' }}
            </button>
            <button type="button" class="btn primary" (click)="save()" [disabled]="isLoading || isSaving">
              {{ isSaving ? 'Salvataggio…' : 'Salva' }}
            </button>
          </div>
        </div>

        <p class="hint" *ngIf="hasError">Errore nel caricamento/salvataggio impostazioni.</p>
        <p class="hint success" *ngIf="!hasError && lastSavedAt">Salvato: {{ lastSavedAt | date: 'dd/MM/yyyy HH:mm' }}</p>

        <div class="form-grid">
          <div class="field">
            <label>Orario apertura</label>
            <input type="time" step="60" [(ngModel)]="openingTime" />
          </div>

          <div class="field">
            <label>Orario chiusura</label>
            <input type="time" step="60" [(ngModel)]="closingTime" />
          </div>

          <div class="field full">
            <p class="hint">{{ openingHoursHint }}</p>
          </div>

          <div class="field full">
            <label class="checkbox">
              <input type="checkbox" [(ngModel)]="orderingPaused" />
              <span>Fermare le ordinazioni (pausa globale)</span>
            </label>
            <p class="hint">Se attivo, le ordinazioni via QR vengono considerate disabilitate lato backend.</p>
          </div>

          <div class="field">
            <label>Ordini del giorno: da</label>
            <input type="time" step="60" [(ngModel)]="ordersViewStartTime" />
          </div>

          <div class="field">
            <label>Ordini del giorno: a</label>
            <input type="time" step="60" [(ngModel)]="ordersViewEndTime" />
          </div>

          <div class="field full">
            <p class="hint">{{ ordersWindowHint }}</p>
          </div>
        </div>
      </section>

      <section class="managers-panel">
        <div class="manager-tabs" role="tablist" aria-label="Gestione menu e postazioni">
          <button
            type="button"
            class="tab"
            [class.active]="activeManager === 'categories'"
            (click)="activeManager = 'categories'"
          >
            Categorie
          </button>
          <button
            type="button"
            class="tab"
            [class.active]="activeManager === 'areas'"
            (click)="activeManager = 'areas'"
          >
            Aree Postazioni
          </button>
        </div>

        <div class="manager-content">
          <app-categories-manager *ngIf="activeManager === 'categories'"></app-categories-manager>
          <app-areas-manager *ngIf="activeManager === 'areas'"></app-areas-manager>
        </div>
      </section>

    </section>
  `,
  styles: [
    '.settings-card{background:linear-gradient(180deg,#ffffff 0%,#f9fbff 100%);border:1px solid #e5e7eb;border-radius:14px;padding:18px;width:100%;max-width:none;box-sizing:border-box;box-shadow:0 8px 24px rgba(15,23,42,.04)}',
    '.header{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;margin-bottom:14px}',
    '.header h2{margin:0;font-size:1.85rem;color:#111827;letter-spacing:-.01em}',
    '.subtitle{color:#6b7280;margin:4px 0 0}',
    '.panel{border:1px solid #e8edf6;border-radius:12px;padding:16px;margin-top:12px;background:#fbfdff}',
    '.panel-header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:12px}',
    '.panel-actions{display:flex;gap:8px;flex-wrap:wrap}',
    '.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:10px}',
    '.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:10px}',
    '.field{display:flex;flex-direction:column;gap:6px}',
    '.field.full{grid-column:1 / -1}',
    'label{font-size:0.9rem;color:#111827;font-weight:600}',
    'input[type="time"]{border:1px solid #d1d5db;border-radius:8px;padding:8px 10px;background:#fff}',
    'input[type="password"]{border:1px solid #d1d5db;border-radius:8px;padding:8px 10px;background:#fff}',
    '.checkbox{display:flex;align-items:center;gap:10px}',
    '.btn{border:1px solid #d1d5db;background:#fff;border-radius:8px;padding:8px 12px;cursor:pointer;transition:all .2s}',
    '.btn:hover:not([disabled]){background:#f7f9fc;border-color:#9ca3af}',
    '.btn.primary{background:#111827;color:#fff;border-color:#111827}',
    '.btn.primary:hover:not([disabled]){background:#1f2937}',
    '.btn[disabled]{opacity:.6;cursor:not-allowed}',
    '.hint{color:#6b7280;font-size:0.9rem;margin:6px 0 0}',
    '.hint.success{color:#065f46}',
    '.managers-panel{margin-top:14px;padding:12px;border:1px solid #e8edf6;border-radius:12px;background:#f8fbff}',
    '.manager-tabs{display:flex;gap:8px;flex-wrap:wrap}',
    '.tab{border:1px solid #d1d5db;background:#fff;color:#334155;border-radius:999px;padding:8px 14px;font-size:.86rem;font-weight:700;cursor:pointer;transition:all .2s}',
    '.tab:hover{background:#f1f5f9}',
    '.tab.active{background:#0f172a;color:#fff;border-color:#0f172a;box-shadow:0 6px 14px rgba(15,23,42,.2)}',
    '.manager-content{margin-top:10px}',
    '@media (max-width: 880px){.settings-card{padding:14px}.grid,.form-grid{grid-template-columns:1fr}}'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsPageComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly dashboard = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  settings: TenantSettings | null = null;

  openingTime = '00:00';
  closingTime = '23:59';
  orderingPaused = false;
  ordersViewStartTime = '00:00';
  ordersViewEndTime = '23:59';

  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';
  isChangingPassword = false;
  passwordError = '';
  passwordSuccess = false;

  isLoading = false;
  isSaving = false;
  hasError = false;
  lastSavedAt: Date | null = null;
  activeManager: 'categories' | 'areas' = 'categories';

  ngOnInit(): void {
    this.loadSettings();
  }

  changePassword(): void {
    this.passwordError = '';
    this.passwordSuccess = false;

    if (!this.currentPassword || !this.newPassword || !this.confirmNewPassword) {
      this.passwordError = 'Compila tutti i campi password.';
      this.cdr.markForCheck();
      return;
    }

    if (this.newPassword.length < 8) {
      this.passwordError = 'La nuova password deve avere almeno 8 caratteri.';
      this.cdr.markForCheck();
      return;
    }

    if (this.newPassword !== this.confirmNewPassword) {
      this.passwordError = 'Le password non coincidono.';
      this.cdr.markForCheck();
      return;
    }

    this.isChangingPassword = true;
    this.cdr.markForCheck();

    this.auth
      .changePassword({
        currentPassword: this.currentPassword,
        newPassword: this.newPassword,
        confirmNewPassword: this.confirmNewPassword
      })
      .pipe(
        finalize(() => {
          this.isChangingPassword = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.passwordSuccess = true;
          this.currentPassword = '';
          this.newPassword = '';
          this.confirmNewPassword = '';
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.passwordError = err?.message || 'Errore durante il cambio password.';
          this.cdr.markForCheck();
        }
      });
  }

  loadSettings(): void {
    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    this.dashboard
      .refreshTenantSettings()
      .pipe(
        timeout(8000),
        retry({ count: 1, delay: 200 }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (settings) => {
          this.settings = settings;
          this.openingTime = settings.openingTime || '00:00';
          this.closingTime = settings.closingTime || '23:59';
          this.orderingPaused = !!settings.orderingPaused;
          this.ordersViewStartTime = settings.ordersViewStartTime || '00:00';
          this.ordersViewEndTime = settings.ordersViewEndTime || '23:59';
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.settings = null;
          this.cdr.markForCheck();
        }
      });
  }

  save(): void {
    this.isSaving = true;
    this.hasError = false;
    this.cdr.markForCheck();

    this.dashboard
      .updateTenantSettings({
        openingTime: this.openingTime,
        closingTime: this.closingTime,
        orderingPaused: this.orderingPaused,
        ordersViewStartTime: this.ordersViewStartTime,
        ordersViewEndTime: this.ordersViewEndTime
      })
      .pipe(
        timeout(8000),
        finalize(() => {
          this.isSaving = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (settings) => {
          this.settings = settings;
          this.lastSavedAt = new Date();
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.cdr.markForCheck();
        }
      });
  }

  get ordersWindowHint(): string {
    const start = this.parseMinutes(this.ordersViewStartTime);
    const end = this.parseMinutes(this.ordersViewEndTime);

    if (start === null || end === null) {
      return 'Questa finestra decide quali ordini rientrano nel giorno operativo nelle viste staff.';
    }

    if (end < start) {
      return `Finestra giorno operativo: dalle ${this.ordersViewStartTime} alle ${this.ordersViewEndTime} del giorno successivo.`;
    }

    return `Finestra giorno operativo: dalle ${this.ordersViewStartTime} alle ${this.ordersViewEndTime} dello stesso giorno.`;
  }

  get openingHoursHint(): string {
    const start = this.parseMinutes(this.openingTime);
    const end = this.parseMinutes(this.closingTime);

    if (start === null || end === null) {
      return 'Questa finestra decide quando il locale risulta aperto alle ordinazioni.';
    }

    if (end < start) {
      return `Apertura operativa: dalle ${this.openingTime} alle ${this.closingTime} del giorno successivo.`;
    }

    return `Apertura operativa: dalle ${this.openingTime} alle ${this.closingTime} dello stesso giorno.`;
  }

  private parseMinutes(value: string): number | null {
    const trimmed = (value || '').trim();
    if (trimmed.length !== 5 || trimmed.charAt(2) !== ':') {
      return null;
    }

    const hh = Number(trimmed.slice(0, 2));
    const mm = Number(trimmed.slice(3, 5));
    if (!Number.isFinite(hh) || !Number.isFinite(mm) || hh < 0 || hh > 23 || mm < 0 || mm > 59) {
      return null;
    }

    return hh * 60 + mm;
  }
}

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, retry, timeout } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import { DashboardService, TenantSettings } from '../../services/dashboard.service';
import { CategoriesManagerComponent } from './categories-manager.component';
import { AreasManagerComponent } from './areas-manager.component';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [DatePipe, FormsModule, CategoriesManagerComponent, AreasManagerComponent],
  template: `
    @if (auth.currentUser(); as user) {
    <div class="sp">

      <!-- PAGE HEADER -->
      <div class="sp-page-head">
        <div>
          <h1 class="sp-page-title">Impostazioni</h1>
          <p class="sp-page-sub">Configurazioni operative del locale</p>
        </div>
        <button type="button" class="sp-logout" (click)="auth.logout()">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          Logout
        </button>
      </div>

      <!-- ACCOUNT -->
      <div class="sp-card">
        <div class="sp-section-label">Account</div>
        <div class="sp-account-row">
          <div class="sp-avatar">{{ (user.firstName || '?')[0] }}{{ (user.lastName || '')[0] }}</div>
          <div class="sp-account-info">
            <div class="sp-account-name">{{ user.firstName }} {{ user.lastName }}</div>
            <div class="sp-account-email">{{ user.email }}</div>
          </div>
        </div>
      </div>

      <!-- BRANDING / LOGO -->
      <div class="sp-card">
        <div class="sp-card-head">
          <div class="sp-section-label">Logo del locale</div>
          <div class="sp-card-actions">
            @if (pendingLogoDataUrl !== undefined) {
              <button type="button" class="sp-btn" (click)="cancelLogo()" [disabled]="isSavingLogo">Annulla</button>
            }
            <button type="button" class="sp-btn sp-btn-primary" (click)="saveLogo()"
              [disabled]="isSavingLogo || pendingLogoDataUrl === undefined">
              {{ isSavingLogo ? 'Salvataggio…' : 'Salva logo' }}
            </button>
          </div>
        </div>

        @if (logoSaveSuccess) {
          <div class="sp-alert sp-alert-ok">✓ Logo aggiornato con successo.</div>
        }
        @if (logoSaveError) {
          <div class="sp-alert sp-alert-err">{{ logoSaveError }}</div>
        }

        <div class="sp-logo-row">
          <div class="sp-logo-preview">
            @if (logoPreviewSrc) {
              <img [src]="logoPreviewSrc" alt="Logo locale" class="sp-logo-img" />
              <button type="button" class="sp-logo-remove" title="Rimuovi logo" (click)="removeLogo()">✕</button>
            } @else {
              <div class="sp-logo-placeholder">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"
                  stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="3"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span>Nessun logo</span>
              </div>
            }
          </div>

          <div class="sp-logo-upload">
            <p class="sp-hint">Carica il logo del tuo locale (PNG, JPG, WebP — max 2 MB).<br>Verrà mostrato nel menu digitale e sui QR code stampati.</p>
            <label class="sp-btn sp-btn-upload">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              Scegli immagine
              <input type="file" accept="image/png,image/jpeg,image/webp,image/gif" (change)="onLogoFileChange($event)" hidden />
            </label>
          </div>
        </div>
      </div>

      <!-- PASSWORD -->
      <div class="sp-card">
        <div class="sp-card-head">
          <div class="sp-section-label">Sicurezza</div>
          <button type="button" class="sp-btn sp-btn-primary" (click)="changePassword()" [disabled]="isChangingPassword">
            {{ isChangingPassword ? 'Aggiornamento…' : 'Aggiorna password' }}
          </button>
        </div>
        @if (passwordSuccess) {
          <div class="sp-alert sp-alert-ok">✓ Password aggiornata con successo.</div>
        }
        @if (passwordError) {
          <div class="sp-alert sp-alert-err">{{ passwordError }}</div>
        }
        <div class="sp-fields">
          <div class="sp-field">
            <label>Password attuale</label>
            <input type="password" [(ngModel)]="currentPassword" autocomplete="current-password" placeholder="••••••••" />
          </div>
          <div class="sp-field">
            <label>Nuova password</label>
            <input type="password" [(ngModel)]="newPassword" autocomplete="new-password" placeholder="Minimo 8 caratteri" />
          </div>
          <div class="sp-field">
            <label>Conferma nuova password</label>
            <input type="password" [(ngModel)]="confirmNewPassword" autocomplete="new-password" placeholder="Ripeti la nuova password" />
          </div>
        </div>
      </div>

      <!-- ORARI + ORDINAZIONI (sezione unificata) -->
      <div class="sp-card">
        <div class="sp-card-head">
          <div class="sp-section-label">Orari e ordinazioni</div>
          <div class="sp-card-actions">
            <button type="button" class="sp-btn" (click)="loadSettings()" [disabled]="isLoading || isSaving">
              {{ isLoading ? 'Caricamento…' : 'Ricarica' }}
            </button>
            <button type="button" class="sp-btn sp-btn-primary" (click)="save()" [disabled]="isLoading || isSaving">
              {{ isSaving ? 'Salvataggio…' : 'Salva' }}
            </button>
          </div>
        </div>

        @if (hasError) {
          <div class="sp-alert sp-alert-err">Errore nel caricamento o salvataggio.</div>
        }
        @if (!hasError && lastSavedAt) {
          <div class="sp-alert sp-alert-ok">✓ Salvato il {{ lastSavedAt | date: 'dd/MM/yyyy HH:mm' }}</div>
        }

        <!-- Apertura -->
        <div class="sp-sub-head">
          <span class="sp-sub-icon">🕐</span> Orario di apertura
        </div>
        <p class="sp-hint">Quando il locale risulta aperto alle ordinazioni via QR.</p>
        <div class="sp-time-row">
          <div class="sp-field sp-time-field">
            <label>Apertura</label>
            <input type="time" step="60" [(ngModel)]="openingTime" />
          </div>
          <div class="sp-time-arrow">→</div>
          <div class="sp-field sp-time-field">
            <label>Chiusura</label>
            <input type="time" step="60" [(ngModel)]="closingTime" />
          </div>
        </div>
        <p class="sp-computed-hint">{{ openingHoursHint }}</p>

        <div class="sp-divider"></div>

        <!-- Finestra ordini -->
        <div class="sp-sub-head">
          <span class="sp-sub-icon">📋</span> Finestra ordini del giorno
        </div>
        <p class="sp-hint">Intervallo usato per raggruppare gli ordini nelle viste staff.</p>
        <div class="sp-time-row">
          <div class="sp-field sp-time-field">
            <label>Da</label>
            <input type="time" step="60" [(ngModel)]="ordersViewStartTime" />
          </div>
          <div class="sp-time-arrow">→</div>
          <div class="sp-field sp-time-field">
            <label>A</label>
            <input type="time" step="60" [(ngModel)]="ordersViewEndTime" />
          </div>
        </div>
        <p class="sp-computed-hint">{{ ordersWindowHint }}</p>

        <div class="sp-divider"></div>

        <!-- Toggle pausa -->
        <div class="sp-toggle-row">
          <div class="sp-toggle-text">
            <div class="sp-toggle-label">Pausa ordinazioni</div>
            <div class="sp-hint">Se attivo, le ordinazioni via QR sono disabilitate globalmente.</div>
          </div>
          <label class="sp-toggle">
            <input type="checkbox" [(ngModel)]="orderingPaused" />
            <span class="sp-toggle-track">
              <span class="sp-toggle-thumb"></span>
            </span>
          </label>
        </div>
      </div>

      <!-- MANAGERS -->
      <div class="sp-card sp-card-no-pad">
        <div class="sp-manager-tabs">
          <button type="button" class="sp-tab" [class.sp-tab-active]="activeManager === 'categories'"
            (click)="activeManager = 'categories'">Categorie</button>
          <button type="button" class="sp-tab" [class.sp-tab-active]="activeManager === 'areas'"
            (click)="activeManager = 'areas'">Aree e postazioni</button>
        </div>
        <div class="sp-manager-body">
          @if (activeManager === 'categories') { <app-categories-manager /> }
          @if (activeManager === 'areas') { <app-areas-manager /> }
        </div>
      </div>

    </div>
    }
  `,
  styles: [`
    .sp {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 20px;
      max-width: 860px;
      margin: 0 auto;
      box-sizing: border-box;
    }

    /* PAGE HEADER */
    .sp-page-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 4px;
    }
    .sp-page-title {
      margin: 0;
      font-size: 1.9rem;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: -.02em;
      line-height: 1.15;
    }
    .sp-page-sub {
      margin: 5px 0 0;
      color: #64748b;
      font-size: 0.9rem;
      font-weight: 500;
    }
    .sp-logout {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 9px 16px;
      border-radius: 999px;
      border: 1.5px solid #e2e8f0;
      background: #fff;
      color: #475569;
      font-size: 0.85rem;
      font-weight: 700;
      cursor: pointer;
      transition: all .18s ease;
      white-space: nowrap;
      flex-shrink: 0;
    }
    .sp-logout:hover { background: #fef2f2; border-color: #fca5a5; color: #dc2626; }

    /* CARD */
    .sp-card {
      background: #fff;
      border: 1.5px solid #e8eef6;
      border-radius: 18px;
      padding: 22px;
      box-shadow: 0 2px 16px rgba(15,23,42,.05);
    }
    .sp-card-no-pad { padding: 0; overflow: hidden; }

    /* CARD HEADER ROW */
    .sp-card-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    .sp-card-actions { display: flex; gap: 8px; }

    /* SECTION LABEL */
    .sp-section-label {
      font-size: 0.72rem;
      font-weight: 800;
      letter-spacing: .1em;
      text-transform: uppercase;
      color: #94a3b8;
      margin-bottom: 14px;
    }

    /* ACCOUNT */
    .sp-account-row {
      display: flex;
      align-items: center;
      gap: 14px;
    }
    .sp-avatar {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      background: linear-gradient(135deg, #3b82f6, #6366f1);
      color: #fff;
      font-size: 1.05rem;
      font-weight: 800;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      text-transform: uppercase;
    }
    .sp-account-name { font-weight: 700; font-size: 1rem; color: #0f172a; }
    .sp-account-email { font-size: 0.85rem; color: #64748b; margin-top: 2px; }

    /* FORM FIELDS */
    .sp-fields {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 14px;
    }
    .sp-field {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .sp-field label {
      font-size: 0.82rem;
      font-weight: 700;
      color: #374151;
    }
    .sp-field input[type="password"],
    .sp-field input[type="time"] {
      border: 1.5px solid #e2e8f0;
      border-radius: 10px;
      padding: 10px 12px;
      background: #f8fafc;
      font-size: 0.92rem;
      color: #0f172a;
      outline: none;
      transition: border-color .15s ease, box-shadow .15s ease;
      font-family: inherit;
      width: 100%;
      box-sizing: border-box;
    }
    .sp-field input:focus {
      border-color: #6366f1;
      box-shadow: 0 0 0 3px rgba(99,102,241,.12);
      background: #fff;
    }

    /* BUTTONS */
    .sp-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 9px 16px;
      border-radius: 10px;
      border: 1.5px solid #e2e8f0;
      background: #fff;
      color: #374151;
      font-size: 0.85rem;
      font-weight: 700;
      cursor: pointer;
      transition: all .18s ease;
      white-space: nowrap;
      font-family: inherit;
    }
    .sp-btn:hover:not([disabled]) { background: #f8fafc; border-color: #cbd5e1; }
    .sp-btn[disabled] { opacity: .5; cursor: not-allowed; }
    .sp-btn-primary {
      background: #0f172a;
      color: #fff;
      border-color: #0f172a;
      box-shadow: 0 4px 12px rgba(15,23,42,.18);
    }
    .sp-btn-primary:hover:not([disabled]) { background: #1e293b; }

    /* ALERTS */
    .sp-alert {
      border-radius: 10px;
      padding: 10px 14px;
      font-size: 0.87rem;
      font-weight: 600;
      margin-bottom: 14px;
    }
    .sp-alert-ok { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .sp-alert-err { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }

    /* SUB SECTIONS */
    .sp-sub-head {
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 0.9rem;
      font-weight: 800;
      color: #1e293b;
      margin-bottom: 4px;
      margin-top: 4px;
    }
    .sp-sub-icon { font-size: 1rem; line-height: 1; }
    .sp-hint {
      font-size: 0.82rem;
      color: #94a3b8;
      font-weight: 500;
      margin: 0 0 12px;
      line-height: 1.5;
    }
    .sp-computed-hint {
      font-size: 0.82rem;
      color: #6366f1;
      font-weight: 600;
      margin: 8px 0 0;
      background: rgba(99,102,241,.06);
      border-radius: 8px;
      padding: 7px 12px;
    }
    .sp-divider {
      height: 1px;
      background: #f1f5f9;
      margin: 20px 0;
    }

    /* TIME ROW */
    .sp-time-row {
      display: flex;
      align-items: flex-end;
      gap: 12px;
    }
    .sp-time-field { flex: 1; }
    .sp-time-arrow {
      font-size: 1.1rem;
      color: #94a3b8;
      padding-bottom: 10px;
      flex-shrink: 0;
    }

    /* TOGGLE */
    .sp-toggle-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .sp-toggle-text { flex: 1; }
    .sp-toggle-label { font-size: 0.9rem; font-weight: 700; color: #1e293b; margin-bottom: 3px; }
    .sp-toggle {
      position: relative;
      display: inline-block;
      width: 48px;
      height: 28px;
      flex-shrink: 0;
      cursor: pointer;
    }
    .sp-toggle input { opacity: 0; width: 0; height: 0; position: absolute; }
    .sp-toggle-track {
      position: absolute;
      inset: 0;
      border-radius: 999px;
      background: #e2e8f0;
      transition: background .2s ease;
    }
    .sp-toggle input:checked + .sp-toggle-track { background: #6366f1; }
    .sp-toggle-thumb {
      position: absolute;
      top: 3px;
      left: 3px;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #fff;
      box-shadow: 0 1px 4px rgba(0,0,0,.15);
      transition: transform .2s cubic-bezier(.34,1.2,.64,1);
    }
    .sp-toggle input:checked ~ .sp-toggle-track .sp-toggle-thumb { transform: translateX(20px); }

    /* MANAGERS */
    .sp-manager-tabs {
      display: flex;
      gap: 0;
      padding: 16px 16px 0;
      border-bottom: 1.5px solid #f1f5f9;
    }
    .sp-tab {
      padding: 10px 18px;
      border: none;
      background: none;
      font-size: 0.88rem;
      font-weight: 700;
      color: #94a3b8;
      cursor: pointer;
      border-bottom: 2.5px solid transparent;
      margin-bottom: -1.5px;
      transition: color .15s ease, border-color .15s ease;
      font-family: inherit;
    }
    .sp-tab:hover { color: #475569; }
    .sp-tab-active { color: #6366f1; border-bottom-color: #6366f1; }
    .sp-manager-body { padding: 16px; }

    /* ── INPUTS BASE ──────────────────────────────────────── */
    input[type="password"],
    input[type="time"] {
      box-sizing: border-box;
      width: 100%;
    }

    /* LOGO BRANDING */
    .sp-logo-row {
      display: flex;
      align-items: flex-start;
      gap: 24px;
      flex-wrap: wrap;
    }
    .sp-logo-preview {
      position: relative;
      width: 120px;
      height: 90px;
      border: 1.5px solid #e2e8f0;
      border-radius: 14px;
      overflow: hidden;
      background: #f8fafc;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .sp-logo-img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      padding: 8px;
    }
    .sp-logo-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      color: #94a3b8;
      font-size: 0.78rem;
      font-weight: 600;
    }
    .sp-logo-remove {
      position: absolute;
      top: 4px;
      right: 4px;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: rgba(15,23,42,.55);
      color: #fff;
      border: none;
      font-size: 0.72rem;
      cursor: pointer;
      display: grid;
      place-items: center;
      line-height: 1;
      transition: background .15s;
    }
    .sp-logo-remove:hover { background: #dc2626; }
    .sp-logo-upload {
      flex: 1;
      min-width: 200px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      justify-content: center;
    }
    .sp-btn-upload {
      cursor: pointer;
      align-self: flex-start;
    }

    @media (max-width: 640px) {
      .sp { padding: 12px; gap: 12px; }
      .sp-card { padding: 14px; }
      .sp-card-no-pad { padding: 0; }

      .sp-page-head { flex-wrap: wrap; gap: 8px; }
      .sp-page-title { font-size: 1.4rem; }
      .sp-logout { padding: 7px 12px; font-size: 0.8rem; }

      .sp-card-head { flex-direction: column; align-items: stretch; gap: 10px; }
      .sp-card-actions { justify-content: flex-end; }

      .sp-time-row { flex-direction: column; gap: 10px; }
      .sp-time-arrow { display: none; }
      .sp-time-field { width: 100%; }

      .sp-fields { grid-template-columns: 1fr; }

      .sp-toggle-row { gap: 12px; }

      .sp-logo-row { flex-direction: column; gap: 16px; }
      .sp-logo-preview { width: 100%; height: 110px; }
      .sp-logo-upload { min-width: unset; }

      .sp-manager-tabs { padding: 12px 12px 0; gap: 0; }
      .sp-tab { padding: 9px 14px; font-size: 0.83rem; }
      .sp-manager-body { padding: 12px; }
    }

    @media (max-width: 380px) {
      .sp-page-head { flex-direction: column; align-items: flex-start; }
      .sp-card { padding: 12px; }
    }
  `],
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

  // branding
  logoPreviewSrc: string | null = null;
  pendingLogoDataUrl: string | null | undefined = undefined; // undefined = no change
  isSavingLogo = false;
  logoSaveSuccess = false;
  logoSaveError = '';

  ngOnInit(): void {
    this.loadSettings();
    const user = this.auth.getCurrentUser();
    if (user?.tenantLogoDataUrl) {
      this.logoPreviewSrc = user.tenantLogoDataUrl;
    }
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

  onLogoFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      this.logoSaveError = 'Il file è troppo grande: massimo 2 MB.';
      this.cdr.markForCheck();
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      this.pendingLogoDataUrl = dataUrl;
      this.logoPreviewSrc = dataUrl;
      this.logoSaveError = '';
      this.logoSaveSuccess = false;
      this.cdr.markForCheck();
    };
    reader.readAsDataURL(file);
  }

  removeLogo(): void {
    this.pendingLogoDataUrl = null;
    this.logoPreviewSrc = null;
    this.logoSaveSuccess = false;
    this.logoSaveError = '';
    this.cdr.markForCheck();
  }

  cancelLogo(): void {
    const user = this.auth.getCurrentUser();
    this.logoPreviewSrc = user?.tenantLogoDataUrl ?? null;
    this.pendingLogoDataUrl = undefined;
    this.logoSaveSuccess = false;
    this.logoSaveError = '';
    this.cdr.markForCheck();
  }

  saveLogo(): void {
    if (this.pendingLogoDataUrl === undefined) return;
    this.isSavingLogo = true;
    this.logoSaveSuccess = false;
    this.logoSaveError = '';
    this.cdr.markForCheck();

    this.dashboard.updateTenantBranding(this.pendingLogoDataUrl)
      .pipe(finalize(() => { this.isSavingLogo = false; this.cdr.markForCheck(); }))
      .subscribe({
        next: () => {
          this.logoSaveSuccess = true;
          this.pendingLogoDataUrl = undefined;
          this.auth.refreshCurrentUser().subscribe();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logoSaveError = err?.error?.message || 'Errore durante il salvataggio del logo.';
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

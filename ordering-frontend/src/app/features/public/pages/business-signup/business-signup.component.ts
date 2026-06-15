import { Component, OnInit } from '@angular/core';
import {
  FormBuilder, FormGroup, Validators, AbstractControl,
  ValidationErrors, ValidatorFn
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import {
  BusinessRegistrationService,
  BusinessSignupRequest,
  BusinessSignupResponse
} from '../../services/business-registration.service';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

// ── Custom validators ─────────────────────────────────

function italianVat(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const v = (ctrl.value || '').trim().toUpperCase().replace(/^IT/, '');
    if (!v) return null; // handled by required
    return /^[0-9]{11}$/.test(v) ? null : { italianVat: true };
  };
}

function phoneNumber(): ValidatorFn {
  // Accept: +39 3xx xxxxxxx  |  +39 0xx xxxxxxxx  |  3xx xxxxxxx  |  0xx xxxxxxxx
  const re = /^(\+?[0-9]{1,3}[\s\-]?)?([0-9][\s\-]?){6,14}[0-9]$/;
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const v = (ctrl.value || '').trim().replace(/\s/g, '');
    if (!v) return null;
    return re.test(v) ? null : { phone: true };
  };
}

function provinceCode(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const v = (ctrl.value || '').trim();
    if (!v) return null;
    return /^[A-Za-z]{2}$/.test(v) ? null : { provinceCode: true };
  };
}

function italianCap(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const v = (ctrl.value || '').trim();
    if (!v) return null;
    return /^\d{5}$/.test(v) ? null : { italianCap: true };
  };
}

function onlyLetters(): ValidatorFn {
  // Allow accented chars, spaces, apostrophes, hyphens
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const v = (ctrl.value || '').trim();
    if (!v) return null;
    return /^[A-Za-zÀ-ÿ\s'\-]+$/.test(v) ? null : { onlyLetters: true };
  };
}

function slugPattern(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const v = (ctrl.value || '').trim();
    if (!v) return null;
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(v)) return { slug: true };
    return null;
  };
}

// ── Component ─────────────────────────────────────────

@Component({
  selector: 'app-business-signup',
  templateUrl: './business-signup.component.html',
  styleUrls: ['./business-signup.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(10px)' }),
        animate('280ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-8px)' }))
      ])
    ]),
    trigger('slideIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-12px)' }),
        animate('250ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-8px)' }))
      ])
    ])
  ]
})
export class BusinessSignupComponent implements OnInit {
  signupForm!: FormGroup;
  currentStep = 1;
  totalSteps = 3;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  companyLogoDataUrl: string | null = null;
  companyLogoName = '';

  businessTypes = [
    { value: 'LIDO',       label: 'Lido / Spiaggia' },
    { value: 'BAR',        label: 'Bar' },
    { value: 'RESTAURANT', label: 'Ristorante' },
    { value: 'NIGHTCLUB',  label: 'Discoteca' },
    { value: 'OTHER',      label: 'Altro' }
  ];

  billingCycles = [
    { value: 'MONTHLY', label: 'Mensile' },
    { value: 'YEARLY',  label: 'Annuale' }
  ];

  planCodes = [
    { value: 'BASIC',        label: 'Piano Base' },
    { value: 'PROFESSIONAL', label: 'Piano Professionale' },
    { value: 'ENTERPRISE',   label: 'Piano Enterprise' }
  ];

  constructor(
    private fb: FormBuilder,
    private businessRegistrationService: BusinessRegistrationService,
    private router: Router
  ) {}

  ngOnInit(): void { this.initializeForm(); }

  // ── Logo ──────────────────────────────────────────

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) { this.companyLogoDataUrl = null; this.companyLogoName = ''; return; }

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Il logo deve essere un file immagine (PNG, JPG, SVG…).';
      input.value = ''; return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.errorMessage = 'Il logo non può superare 2 MB.';
      input.value = ''; return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.companyLogoDataUrl = typeof reader.result === 'string' ? reader.result : null;
      this.companyLogoName = file.name;
    };
    reader.onerror = () => {
      this.errorMessage = 'Impossibile leggere il file del logo.';
      this.companyLogoDataUrl = null;
      this.companyLogoName = '';
      input.value = '';
    };
    reader.readAsDataURL(file);
  }

  // ── Auto-format helpers ───────────────────────────

  autoUppercase(field: string, event: Event): void {
    const v = (event.target as HTMLInputElement).value.toUpperCase();
    this.signupForm.get(field)?.setValue(v, { emitEvent: false });
  }

  autoLowercase(field: string, event: Event): void {
    const v = (event.target as HTMLInputElement).value.toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9-]/g, '');
    this.signupForm.get(field)?.setValue(v, { emitEvent: false });
  }

  suggestSlug(): void {
    const slug = this.signupForm.get('requestedSlug');
    if (slug?.value) return; // already filled
    const name: string = this.signupForm.get('tenantName')?.value || '';
    const suggested = name.toLowerCase()
      .normalize('NFD').replace(/[̀-ͯ]/g, '') // strip accents
      .replace(/[^a-z0-9\s-]/g, '')
      .trim()
      .replace(/\s+/g, '-')
      .replace(/-{2,}/g, '-');
    if (suggested) slug?.setValue(suggested);
  }

  // ── Character count helper ────────────────────────

  charCount(field: string): number {
    return (this.signupForm.get(field)?.value || '').length;
  }

  // ── Navigation ────────────────────────────────────

  nextStep(): void {
    this.touchStepFields(this.currentStep);
    if (this.isValidStep(this.currentStep)) {
      this.currentStep++;
      this.errorMessage = '';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      this.errorMessage = 'Correggi i campi evidenziati prima di continuare.';
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) { this.currentStep--; this.errorMessage = ''; }
  }

  // ── Submit ────────────────────────────────────────

  submitForm(): void {
    this.touchStepFields(this.currentStep);
    if (!this.signupForm.valid) {
      this.errorMessage = 'Correggi i campi evidenziati prima di inviare.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';

    const raw = this.signupForm.getRawValue();
    const request: BusinessSignupRequest = {
      ...raw,
      province: (raw.province || '').toUpperCase(),
      vatNumber: (raw.vatNumber || '').toUpperCase().replace(/^IT/, ''),
      companyLogoDataUrl: this.companyLogoDataUrl ?? undefined
    };

    this.businessRegistrationService.submitBusinessRegistration(request).subscribe({
      next: (response: BusinessSignupResponse) => {
        this.isLoading = false;
        this.successMessage = response.checkoutUrl
          ? 'Registrazione completata! Stai per essere reindirizzato al pagamento...'
          : response.message;

        if (response.checkoutUrl) {
          setTimeout(() => { window.location.href = response.checkoutUrl!; }, 1500);
        } else {
          setTimeout(() => this.router.navigate(['/public/signup-success']), 2000);
        }
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Errore durante la registrazione. Riprova.';
      }
    });
  }

  // ── Helpers ───────────────────────────────────────

  isFieldInvalid(name: string): boolean {
    const f = this.signupForm.get(name);
    return !!(f && f.invalid && f.touched);
  }

  isFieldOk(name: string): boolean {
    const f = this.signupForm.get(name);
    return !!(f && f.valid && f.touched && f.value);
  }

  getFieldError(name: string): string {
    const f = this.signupForm.get(name);
    if (!f || !f.errors || !f.touched) return '';
    const e = f.errors;

    // Field-specific messages first
    if (e['required'])     return 'Campo obbligatorio';
    if (e['email'])        return 'Inserisci un\'indirizzo email valido (es. nome@dominio.it)';
    if (e['italianVat'])   return 'Partita IVA non valida: devono essere 11 cifre (es. 12345678901)';
    if (e['phone'])        return 'Numero di telefono non valido (es. +39 333 1234567 o 06 12345678)';
    if (e['provinceCode']) return 'Inserisci la sigla della provincia (2 lettere, es. RM)';
    if (e['italianCap'])   return 'CAP non valido: deve essere composto da 5 cifre (es. 00100)';
    if (e['onlyLetters'])  return 'Sono ammesse solo lettere, spazi e apostrofi';
    if (e['slug'])         return 'Solo lettere minuscole, numeri e trattini (es. il-mio-locale)';
    if (e['minlength'])    return `Minimo ${e['minlength'].requiredLength} caratteri`;
    if (e['maxlength'])    return `Massimo ${e['maxlength'].requiredLength} caratteri`;
    if (e['pattern'])      return 'Formato non valido';
    return 'Valore non valido';
  }

  get stepProgress(): number {
    return (this.currentStep / this.totalSteps) * 100;
  }

  // ── Private ───────────────────────────────────────

  private initializeForm(): void {
    this.signupForm = this.fb.group({
      // Step 1 – Azienda
      tenantName:     ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      legalName:      ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
      businessType:   ['', Validators.required],
      vatNumber:      ['', [Validators.required, italianVat()]],
      businessEmail:  ['', [Validators.required, Validators.email]],
      businessPhone:  ['', [Validators.required, phoneNumber()]],
      // Step 2 – Indirizzo
      addressLine1:   ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
      addressLine2:   ['', Validators.maxLength(100)],               // opzionale
      city:           ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      province:       ['', [Validators.required, provinceCode()]],
      postalCode:     ['', [Validators.required, italianCap()]],
      country:        [{ value: 'Italy', disabled: false }, [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      // Step 3 – Contatto & accesso
      contactFirstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100), onlyLetters()]],
      contactLastName:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100), onlyLetters()]],
      contactEmail:     ['', [Validators.required, Validators.email]],
      contactPhone:     ['', [Validators.required, phoneNumber()]],
      requestedSlug:    ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100), slugPattern()]],
      requestedPlanCode: ['BASIC', Validators.required],
      billingCycle:      ['MONTHLY', Validators.required]
    });
  }

  private getFieldsByStep(step: number): string[] {
    switch (step) {
      case 1: return ['tenantName', 'legalName', 'businessType', 'vatNumber', 'businessEmail', 'businessPhone'];
      case 2: return ['addressLine1', 'city', 'province', 'postalCode', 'country'];
      case 3: return ['contactFirstName', 'contactLastName', 'contactEmail', 'contactPhone', 'requestedSlug', 'requestedPlanCode', 'billingCycle'];
      default: return [];
    }
  }

  private isValidStep(step: number): boolean {
    return this.getFieldsByStep(step).every(name => {
      const c = this.signupForm.get(name);
      return c && c.valid;
    });
  }

  private touchStepFields(step: number): void {
    this.getFieldsByStep(step).forEach(name => {
      this.signupForm.get(name)?.markAsTouched();
    });
  }
}

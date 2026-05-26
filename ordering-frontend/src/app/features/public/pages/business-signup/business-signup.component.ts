import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import { BusinessRegistrationService, BusinessSignupRequest, BusinessSignupResponse } from '../../services/business-registration.service';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

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
        animate('300ms ease-in', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-out', style({ opacity: 0, transform: 'translateY(-10px)' }))
      ])
    ]),
    trigger('slideIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(-20px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ opacity: 0, transform: 'translateX(-20px)' }))
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
    { value: 'LIDO', label: 'Lido / Spiaggia' },
    { value: 'BAR', label: 'Bar' },
    { value: 'RESTAURANT', label: 'Ristorante' },
    { value: 'NIGHTCLUB', label: 'Discoteca' },
    { value: 'OTHER', label: 'Altro' }
  ];

  billingCycles = [
    { value: 'MONTHLY', label: 'Mensile' },
    { value: 'YEARLY', label: 'Annuale' }
  ];

  planCodes = [
    { value: 'BASIC', label: 'Piano Base' },
    { value: 'PROFESSIONAL', label: 'Piano Professionale' },
    { value: 'ENTERPRISE', label: 'Piano Enterprise' }
  ];

  constructor(
    private fb: FormBuilder,
    private businessRegistrationService: BusinessRegistrationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      this.companyLogoDataUrl = null;
      this.companyLogoName = '';
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Il logo deve essere un file immagine valido.';
      input.value = '';
      return;
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

  private initializeForm(): void {
    this.signupForm = this.fb.group({
      tenantName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
      legalName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
      businessType: ['', Validators.required],
      vatNumber: ['', [Validators.required, Validators.maxLength(50)]],
      businessEmail: ['', [Validators.required, Validators.email]],
      businessPhone: ['', [Validators.required, Validators.maxLength(50)]],
      addressLine1: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
      addressLine2: ['', [Validators.required, Validators.maxLength(255)]],
      city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      province: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      postalCode: ['', [Validators.required, Validators.pattern(/^\d{5}$/)]],
      country: [{ value: 'Italy', disabled: false }, [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      contactFirstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      contactLastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      contactEmail: ['', [Validators.required, Validators.email]],
      contactPhone: ['', [Validators.required, Validators.maxLength(50)]],
      requestedSlug: ['', [Validators.required, Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/), Validators.minLength(3), Validators.maxLength(100)]],
      requestedPlanCode: ['BASIC', Validators.required],
      billingCycle: ['MONTHLY', Validators.required]
    });
  }

  nextStep(): void {
    if (this.isValidStep(this.currentStep)) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
        this.errorMessage = '';
      }
    } else {
      this.errorMessage = 'Per favore compila tutti i campi obbligatori correttamente';
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.errorMessage = '';
    }
  }

  private isValidStep(step: number): boolean {
    const fields = this.getFieldsByStep(step);
    return fields.every(field => {
      const control = this.signupForm.get(field);
      return control && control.valid;
    });
  }

  private getFieldsByStep(step: number): string[] {
    switch (step) {
      case 1:
        return ['tenantName', 'legalName', 'businessType', 'vatNumber', 'businessEmail', 'businessPhone'];
      case 2:
        return ['addressLine1', 'addressLine2', 'city', 'province', 'postalCode', 'country'];
      case 3:
        return ['contactFirstName', 'contactLastName', 'contactEmail', 'contactPhone', 'requestedSlug', 'requestedPlanCode', 'billingCycle'];
      default:
        return [];
    }
  }

  submitForm(): void {
    if (!this.signupForm.valid) {
      this.errorMessage = 'Per favore compila tutti i campi obbligatori correttamente';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const request: BusinessSignupRequest = {
      ...this.signupForm.getRawValue(),
      country: this.signupForm.get('country')?.value,
      companyLogoDataUrl: this.companyLogoDataUrl ?? undefined
    };

    this.businessRegistrationService.submitBusinessRegistration(request).subscribe({
      next: (response: BusinessSignupResponse) => {
        this.isLoading = false;
        this.successMessage = response.message;
        setTimeout(() => {
          this.router.navigate(['/public/signup-success']);
        }, 2000);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Errore durante la registrazione. Per favore riprova.';
      }
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.signupForm.get(fieldName);
    if (field && field.errors && field.touched) {
      if (field.errors['required']) return 'Campo obbligatorio';
      if (field.errors['minlength']) return `Minimo ${field.errors['minlength'].requiredLength} caratteri`;
      if (field.errors['maxlength']) return `Massimo ${field.errors['maxlength'].requiredLength} caratteri`;
      if (field.errors['email']) return 'Email non valida';
      if (field.errors['pattern']) return 'Formato non valido';
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.signupForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  get stepProgress(): number {
    return (this.currentStep / this.totalSteps) * 100;
  }
}

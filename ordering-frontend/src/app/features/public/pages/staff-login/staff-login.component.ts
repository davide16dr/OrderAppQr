import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { trigger, transition, style, animate } from '@angular/animations';
import { AuthService, LoginRequest } from '../../../../core/services/auth.service';
import { FormFieldComponent } from '../../../../shared/components/form-field/form-field.component';

@Component({
  selector: 'app-staff-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './staff-login.component.html',
  styleUrl: './staff-login.component.scss',
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(20px)' }),
        animate('400ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ opacity: 0, transform: 'translateY(-20px)' }))
      ])
    ]),
    trigger('slideIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(-30px)' }),
        animate('400ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
      ])
    ]),
    trigger('shakeError', [
      transition('* => error', [
        animate('0.5s', style({ transform: 'translateX(-5px)' })),
        animate('0.5s', style({ transform: 'translateX(5px)' })),
        animate('0.5s', style({ transform: 'translateX(0)' }))
      ])
    ])
  ]
})
export class StaffLoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  loginForm!: FormGroup;
  
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly showPassword = signal(false);
  readonly rememberMe = signal(false);
  
  readonly errorAnimationState = signal<string | null>(null);

  ngOnInit(): void {
    this.initializeForm();
    this.loadRememberedEmail();
  }

  private initializeForm(): void {
    this.loginForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(6)
      ]],
      rememberMe: [false]
    });
  }

  private loadRememberedEmail(): void {
    const rememberedEmail = localStorage.getItem('staff_remembered_email');
    if (rememberedEmail) {
      this.loginForm.patchValue({
        email: rememberedEmail,
        rememberMe: true
      });
    }
  }

  onLogin(): void {
    if (!this.loginForm.valid) {
      this.errorMessage.set('Per favore compila tutti i campi correttamente');
      this.triggerErrorAnimation();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const credentials: LoginRequest = this.loginForm.getRawValue();

    this.authService.login(credentials).subscribe({
      next: (resp) => {
        this.isLoading.set(false);

        // Salva email se ricordare è selezionato
        if (this.loginForm.get('rememberMe')?.value) {
          localStorage.setItem('staff_remembered_email', credentials.email);
        } else {
          localStorage.removeItem('staff_remembered_email');
        }

        // If backend suggested a redirect, follow it; otherwise use role-aware client redirect
        const redirect = resp?.redirectUrl ?? this.authService.getDefaultRouteForCurrentUser();
        this.router.navigate([redirect]);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(error.message || 'Errore durante il login');
        this.triggerErrorAnimation();
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword.set(!this.showPassword());
  }

  clearError(): void {
    this.errorMessage.set('');
  }

  private triggerErrorAnimation(): void {
    this.errorAnimationState.set('error');
    setTimeout(() => {
      this.errorAnimationState.set(null);
    }, 500);
  }

  get emailControl() {
    return this.loginForm.get('email');
  }

  get passwordControl() {
    return this.loginForm.get('password');
  }

  get isFormValid(): boolean {
    return this.loginForm.valid && !this.isLoading();
  }
}

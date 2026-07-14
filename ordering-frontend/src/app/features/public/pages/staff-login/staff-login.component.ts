import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService, LoginRequest } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-staff-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './staff-login.component.html',
  styleUrl: './staff-login.component.scss'
})
export class StaffLoginComponent implements OnInit {
  private readonly fb          = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router      = inject(Router);
  private readonly route       = inject(ActivatedRoute);

  loginForm!: FormGroup;

  readonly isLoading    = signal(false);
  readonly errorMessage = signal('');
  readonly showPassword = signal(false);
  readonly isDemoMode   = signal(false);

  ngOnInit(): void {
    this.initializeForm();
    this.loadRememberedEmail();
    if (this.route.snapshot.queryParamMap.get('demo') === 'true') {
      this.isDemoMode.set(true);
      this.loginForm.patchValue({ email: 'demo@orderappqr.it', password: 'Demo2024!' });
      this.onLogin();
    }
  }

  private initializeForm(): void {
    this.loginForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)
      ]],
      password:   ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  private loadRememberedEmail(): void {
    const saved = localStorage.getItem('staff_remembered_email');
    if (saved) {
      this.loginForm.patchValue({ email: saved, rememberMe: true });
    }
  }

  onLogin(): void {
    if (!this.loginForm.valid) {
      this.loginForm.markAllAsTouched();
      this.errorMessage.set('Compila tutti i campi correttamente');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const credentials: LoginRequest = this.loginForm.getRawValue();

    this.authService.login(credentials).subscribe({
      next: (resp) => {
        this.isLoading.set(false);
        if (this.loginForm.get('rememberMe')?.value) {
          localStorage.setItem('staff_remembered_email', credentials.email);
        } else {
          localStorage.removeItem('staff_remembered_email');
        }
        const redirect = resp?.redirectUrl ?? this.authService.getDefaultRouteForCurrentUser();
        this.router.navigate([redirect]);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(error.message || 'Credenziali non valide');
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword.set(!this.showPassword());
  }

  clearError(): void {
    this.errorMessage.set('');
  }

  get emailControl()    { return this.loginForm.get('email'); }
  get passwordControl() { return this.loginForm.get('password'); }
  get isFormValid(): boolean { return this.loginForm.valid && !this.isLoading(); }
}

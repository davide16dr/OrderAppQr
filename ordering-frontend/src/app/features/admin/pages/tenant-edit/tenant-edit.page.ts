import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminTenantService } from '../../services/admin-tenant.service';

@Component({
  selector: 'app-tenant-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tenant-edit.page.html',
  styleUrls: ['./tenant-edit.page.scss']
})
export class TenantEditPageComponent {
  readonly fb = inject(FormBuilder);
  readonly service = inject(AdminTenantService);
  readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);

  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    slug: ['', Validators.required],
    subdomain: ['', Validators.required],
    country: ['IT']
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.getTenant(id).subscribe({
      next: (t) => {
        this.form.patchValue(t as any);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Errore nel caricamento del tenant');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  save() {
    if (this.form.invalid) {
      this.error.set('Compilare tutti i campi obbligatori');
      return;
    }
    
    this.saving.set(true);
    this.error.set(null);
    this.success.set(false);

    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.updateTenant(id, this.form.value as unknown as Partial<any>).subscribe({
      next: () => {
        this.success.set(true);
        this.saving.set(false);
        // Aggiorna il signal del service
        this.service.loadTenants();
        // Naviga indietro dopo 1 secondo
        setTimeout(() => this.router.navigate(['/admin/tenants']), 1000);
      },
      error: (err) => {
        this.error.set('Errore nel salvataggio del tenant');
        this.saving.set(false);
        console.error(err);
      }
    });
  }

  cancel() {
    this.router.navigate(['/admin/tenants']);
  }
}

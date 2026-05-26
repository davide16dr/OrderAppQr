import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminTenantService } from '../../services/admin-tenant.service';

@Component({
  selector: 'app-tenant-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tenant-create.page.html',
  styleUrls: ['./tenant-create.page.scss']
})
export class TenantCreatePageComponent {
  readonly fb = inject(FormBuilder);
  readonly service = inject(AdminTenantService);
  readonly router = inject(Router);

  form = this.fb.group({
    name: ['', [Validators.required]],
    slug: ['', [Validators.required]],
    subdomain: ['', [Validators.required]],
    country: ['IT']
  });

  creating = false;

  submit() {
    if (this.form.invalid) return;
    this.creating = true;
    this.service.createTenant(this.form.value as unknown as Partial<any>).subscribe({
      next: () => this.router.navigate(['/admin/tenants']),
      error: () => this.creating = false
    });
  }
}

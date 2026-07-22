import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { environment } from '../../../../environments/environment';

export interface Tenant {
  id: number;
  name: string;
  slug: string;
  enabled: boolean;
  subdomain?: string;

  /* Dati aziendali */
  legalName?: string | null;
  businessType?: string | null;
  businessEmail?: string | null;
  businessPhone?: string | null;
  vatNumber?: string | null;

  /* Sede legale */
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  province?: string | null;
  postalCode?: string | null;
  country?: string | null;

  /* Contatto principale */
  contactFirstName?: string | null;
  contactLastName?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;

  /* Abbonamento */
  subscriptionPlan?: string | null;
  subscriptionStartDate?: string | null;
  subscriptionEndDate?: string | null;
  subscriptionStatus?: string | null;
  subscriptionPaymentStatus?: string | null;
  cancelAtPeriodEnd?: boolean;
  paymentMethod?: string | null;
}

interface TenantPageResponse {
  content: Tenant[];
}

@Injectable({
  providedIn: 'root',
})
export class AdminTenantService {
  private apiUrl = `${environment.apiUrl}/api/admin/tenants`;
  
  private tenantsSignal = signal<Tenant[]>([]);
  private loadingSignal = signal(false);
  private errorSignal = signal<string | null>(null);
  
  // Computed signal per accesso pubblico
  tenants = computed(() => this.tenantsSignal());
  loading = computed(() => this.loadingSignal());
  error = computed(() => this.errorSignal());

  constructor(private http: HttpClient) {
    this.loadTenants();
  }

  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[] | TenantPageResponse>(this.apiUrl).pipe(
      map((response) => Array.isArray(response) ? response : (response.content ?? []))
    );
  }

  loadTenants(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.getTenants().subscribe({
      next: (data) => {
        this.tenantsSignal.set(data);
        this.loadingSignal.set(false);
      },
      error: (err) => {
        this.errorSignal.set('Errore nel caricamento dei tenant');
        this.loadingSignal.set(false);
        console.error('Error loading tenants:', err);
      }
    });
  }

  updateTenantStatus(tenantId: number, enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${tenantId}/status`, { enabled });
  }

  renewManually(tenantId: number, billingCycle: 'MONTHLY' | 'YEARLY'): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${tenantId}/renew`, { billingCycle });
  }

  expireSubscription(tenantId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${tenantId}/expire`, {});
  }

  updateTenantStatusAndRefresh(tenantId: number, enabled: boolean): void {
    this.updateTenantStatus(tenantId, enabled).subscribe({
      next: () => {
        // Aggiorna il signal locale istantaneamente per reattività
        const updated = this.tenantsSignal().map(t => 
          t.id === tenantId ? { ...t, enabled } : t
        );
        this.tenantsSignal.set(updated);
      },
      error: (err) => {
        this.errorSignal.set('Errore nell\'aggiornamento dello stato');
        console.error('Error updating tenant status:', err);
      }
    });
  }

  getTenant(tenantId: number) {
    return this.http.get<Tenant>(`${this.apiUrl}/${tenantId}`);
  }

  createTenant(payload: Partial<Tenant>) {
    return this.http.post<Tenant>(this.apiUrl, payload);
  }

  updateTenant(tenantId: number, payload: Partial<Tenant>) {
    return this.http.put<Tenant>(`${this.apiUrl}/${tenantId}`, payload);
  }
}

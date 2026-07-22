import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface SubscriptionDto {
  id: number;
  planCode: string | null;
  planName: string | null;
  priceMonthly: number | null;
  priceYearly: number | null;
  status: string;
  paymentStatus: string;
  billingCycle: string;
  currentPeriodEnd: string | null;
  trialEndsAt?: string | null;
  activatedAt: string | null;
  cancelAtPeriodEnd: boolean;
  hasStripeSubscription: boolean;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private base = `${environment.apiUrl}/api/tenant/subscriptions`;

  constructor(private http: HttpClient) {}

  getCurrent(tenantId: string): Observable<SubscriptionDto> {
    return this.http.get<SubscriptionDto>(`${this.base}/current`, { params: { tenantId } });
  }

  cancel(tenantId: string): Observable<SubscriptionDto> {
    return this.http.post<SubscriptionDto>(`${this.base}/cancel`, null, { params: { tenantId } });
  }

  reactivate(tenantId: string): Observable<SubscriptionDto> {
    return this.http.post<SubscriptionDto>(`${this.base}/reactivate`, null, { params: { tenantId } });
  }

  changeBilling(tenantId: string, billingCycle: string): Observable<SubscriptionDto> {
    return this.http.post<SubscriptionDto>(`${this.base}/change-billing`, null, {
      params: { tenantId, billingCycle }
    });
  }

  createPortalSession(tenantId: string, returnUrl: string): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.base}/portal-session`, null, {
      params: { tenantId, returnUrl }
    });
  }

  createCheckout(tenantId: string, billingCycle: string, customerEmail: string): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.base}/checkout`, null, {
      params: { tenantId, billingCycle, customerEmail }
    });
  }
}

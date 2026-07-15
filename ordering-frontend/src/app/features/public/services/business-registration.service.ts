import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface BusinessSignupRequest {
  // Business data
  tenantName: string;
  legalName: string;
  companyLogoDataUrl?: string;
  companyBannerDataUrl?: string;
  businessType: string;
  vatNumber: string;
  businessEmail: string;
  businessPhone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  province: string;
  postalCode: string;
  country: string;
  requestedSlug: string;
  requestedPlanCode: string;

  // Primary contact data
  contactFirstName: string;
  contactLastName: string;
  contactEmail: string;
  contactPhone: string;
  password?: string;
  confirmPassword?: string;

  // Optional
  billingCycle: string;
}

export interface BusinessSignupResponse {
  tenantId: number | null;
  tenantSlug: string;
  tenantStatus: string;
  message: string;
  subscriptionId: number | null;
  checkoutUrl: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class BusinessRegistrationService {
  private apiUrl = `${environment.apiUrl}/api/public/business-registration`;

  constructor(private http: HttpClient) {}

  submitBusinessRegistration(request: BusinessSignupRequest): Observable<BusinessSignupResponse> {
    return this.http.post<BusinessSignupResponse>(`${this.apiUrl}/signup`, request);
  }
}

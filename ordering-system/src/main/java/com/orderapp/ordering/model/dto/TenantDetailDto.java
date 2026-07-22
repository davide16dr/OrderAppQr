package com.orderapp.ordering.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantDetailDto {
    private Long id;
    private String name;
    private String slug;
    private String subdomain;
    private boolean enabled;

    // Business
    private String legalName;
    private String businessType;
    private String businessEmail;
    private String businessPhone;
    private String vatNumber;

    // Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String province;
    private String postalCode;
    private String country;

    // Primary contact (from StaffUser)
    private String contactFirstName;
    private String contactLastName;
    private String contactEmail;
    private String contactPhone;

    // Subscription
    private String subscriptionPlan;
    private String subscriptionStartDate;
    private String subscriptionEndDate;
    private String subscriptionStatus;
    private String subscriptionPaymentStatus;
    private boolean cancelAtPeriodEnd;
    private String paymentMethod;
}

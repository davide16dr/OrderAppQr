package com.orderapp.ordering.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TenantSubscriptionResponseDto {
    private Long id;
    private String planCode;
    private String planName;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String status;
    private String paymentStatus;
    private String billingCycle;
    private String currentPeriodEnd;
    private String activatedAt;
    private boolean cancelAtPeriodEnd;
    private boolean hasStripeSubscription;
}

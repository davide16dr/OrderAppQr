package com.orderapp.ordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSignupResponse {
    private Long tenantId;
    private String tenantSlug;
    private String tenantStatus;
    private String message;
    private Long subscriptionId;
    private String checkoutUrl;
}

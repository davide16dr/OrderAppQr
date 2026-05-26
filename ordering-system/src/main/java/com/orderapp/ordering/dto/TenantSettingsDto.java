package com.orderapp.ordering.dto;

public record TenantSettingsDto(
        String openingTime,
        String closingTime,
        boolean orderingPaused,
        String ordersViewStartTime,
        String ordersViewEndTime
) {
}

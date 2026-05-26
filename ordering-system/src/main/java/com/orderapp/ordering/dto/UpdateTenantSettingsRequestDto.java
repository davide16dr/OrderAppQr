package com.orderapp.ordering.dto;

public record UpdateTenantSettingsRequestDto(
        String openingTime,
        String closingTime,
        Boolean orderingPaused,
        String ordersViewStartTime,
        String ordersViewEndTime
) {
}

package com.orderapp.ordering.dto;

import java.time.OffsetDateTime;

public record AreaResponse(
        Long id,
        Long tenantId,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
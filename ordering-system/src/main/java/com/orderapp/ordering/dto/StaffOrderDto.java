package com.orderapp.ordering.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record StaffOrderDto(
        Long id,
        String code,
        String locationLabel,
        String areaName,
        String type,
        String status,
        String note,
        BigDecimal total,
        OffsetDateTime createdAt,
        List<StaffOrderLineDto> items
) {
}

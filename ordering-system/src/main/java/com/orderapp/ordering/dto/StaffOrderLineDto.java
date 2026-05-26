package com.orderapp.ordering.dto;

import java.math.BigDecimal;

public record StaffOrderLineDto(
        Integer quantity,
        String name,
        BigDecimal total,
        String variant
) {
}

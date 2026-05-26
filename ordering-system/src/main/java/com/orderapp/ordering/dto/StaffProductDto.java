package com.orderapp.ordering.dto;

import java.math.BigDecimal;

public record StaffProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        String category,
        String department,
        BigDecimal vatRate,
        String status,
        boolean availableForOrder,
        Integer variantsCount,
        Integer extrasCount,
        String sku
) {
}

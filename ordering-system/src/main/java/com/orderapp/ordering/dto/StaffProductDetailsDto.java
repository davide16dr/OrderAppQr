package com.orderapp.ordering.dto;

import java.math.BigDecimal;
import java.util.List;

public record StaffProductDetailsDto(
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
        String sku,
        List<CreateTenantProductRequestDto.VariantEntry> variants,
        List<CreateTenantProductRequestDto.VariantEntry> extras
) {
}

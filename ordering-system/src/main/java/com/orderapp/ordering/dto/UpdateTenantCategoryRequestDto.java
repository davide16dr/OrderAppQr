package com.orderapp.ordering.dto;

public record UpdateTenantCategoryRequestDto(
    String name,
    String description,
    Integer displayOrder,
    String status
) {}

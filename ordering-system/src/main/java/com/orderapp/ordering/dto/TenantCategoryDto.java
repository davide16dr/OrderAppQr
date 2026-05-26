package com.orderapp.ordering.dto;

public record TenantCategoryDto(
    long id,
    String name,
    String description,
    int displayOrder,
    String status
) {}

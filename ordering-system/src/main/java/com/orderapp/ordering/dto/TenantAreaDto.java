package com.orderapp.ordering.dto;

public record TenantAreaDto(
    long id,
    String name,
    int displayOrder,
    String status
) {}

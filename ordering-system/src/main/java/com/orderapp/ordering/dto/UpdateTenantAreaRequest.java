package com.orderapp.ordering.dto;

import jakarta.validation.constraints.Size;

public record UpdateTenantAreaRequest(
    @Size(max = 150) String name,
    Integer displayOrder,
    @Size(max = 20) String status
) {}
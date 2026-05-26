package com.orderapp.ordering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantAreaRequest(
    @NotBlank @Size(max = 150) String name
) {}

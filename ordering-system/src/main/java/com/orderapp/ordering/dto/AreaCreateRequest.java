package com.orderapp.ordering.dto;

import jakarta.validation.constraints.NotBlank;

public record AreaCreateRequest(
        @NotBlank(message = "Nome area obbligatorio") String name,
        String description
) {
}
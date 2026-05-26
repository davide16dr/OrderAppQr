package com.orderapp.ordering.dto;

import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StationCreateRequest(
        @NotBlank(message = "Nome postazione obbligatorio") String name,
        @NotNull(message = "Tipo postazione obbligatorio") StationType type,
        @NotNull(message = "Area obbligatoria") Long areaId,
        @NotNull(message = "Capacità obbligatoria") @Min(value = 1, message = "Capacità deve essere > 0") Integer capacity,
        @NotNull(message = "Stato obbligatorio") StationStatus status,
        String notes,
        Boolean active,
        Boolean generateQrAutomatically
) {
}
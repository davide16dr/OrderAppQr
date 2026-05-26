package com.orderapp.ordering.dto;

import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StationUpdateRequest(
        @NotBlank(message = "Nome postazione obbligatorio") String name,
        StationType type,
        Long areaId,
        @Min(value = 1, message = "Capacità deve essere > 0") Integer capacity,
        StationStatus status,
        String notes,
        Boolean active
) {
}
package com.orderapp.ordering.dto;

import java.time.OffsetDateTime;

import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;

public record StationResponse(
        Long id,
        Long tenantId,
        String name,
        StationType type,
        Long areaId,
        String areaName,
        Integer capacity,
        StationStatus status,
        boolean active,
        String qrCode,
        String qrUrl,
        boolean qrActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
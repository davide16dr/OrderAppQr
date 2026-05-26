package com.orderapp.ordering.dto;

import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;

public record StationSummaryResponse(
        Long id,
        String name,
        StationType type,
        Long areaId,
        String areaName,
        Integer capacity,
        StationStatus status,
        boolean active,
        String activeQrCode,
        long activeOrdersCount
) {
}
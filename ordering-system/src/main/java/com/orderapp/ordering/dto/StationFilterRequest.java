package com.orderapp.ordering.dto;

import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;

public record StationFilterRequest(
        String name,
        Long areaId,
        StationType type,
        StationStatus status,
        Boolean active
) {
}
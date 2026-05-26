package com.orderapp.ordering.dto;

public record ResolveQrResponse(
        Long tenantId,
        String tenantSlug,
        String tenantName,
        Long stationId,
        String code,
        String stationName,
        String stationType,
        Long areaId,
        String areaName,
        boolean tenantActive,
        boolean stationActive,
        boolean qrActive,
        boolean orderingEnabled,
        String menuUrl
) {
}
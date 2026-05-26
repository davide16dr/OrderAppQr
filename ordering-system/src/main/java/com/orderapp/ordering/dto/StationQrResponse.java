package com.orderapp.ordering.dto;

import java.time.OffsetDateTime;

public record StationQrResponse(
        Long id,
        Long stationId,
        String code,
        String qrValue,
        String qrImageBase64,
        boolean active,
        OffsetDateTime generatedAt,
        OffsetDateTime regeneratedAt,
        String downloadUrl
) {
}
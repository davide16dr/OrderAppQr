package com.orderapp.ordering.dto;

public record GenerateQrResponse(
        StationResponse station,
        StationQrResponse qr
) {
}
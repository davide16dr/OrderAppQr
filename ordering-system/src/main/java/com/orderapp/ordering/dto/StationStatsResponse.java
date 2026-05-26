package com.orderapp.ordering.dto;

public record StationStatsResponse(
        long total,
        long available,
        long occupied,
        long reserved,
        long orderingDisabled,
        long closed,
        long activeOrders
) {
}
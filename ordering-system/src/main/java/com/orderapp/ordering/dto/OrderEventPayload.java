package com.orderapp.ordering.dto;

public record OrderEventPayload(
    long orderId,
    long tenantId,
    String status,
    String eventType
) {
    public static OrderEventPayload created(long orderId, long tenantId) {
        return new OrderEventPayload(orderId, tenantId, "NEW", "ORDER_CREATED");
    }

    public static OrderEventPayload statusChanged(long orderId, long tenantId, String status) {
        return new OrderEventPayload(orderId, tenantId, status, "STATUS_CHANGED");
    }
}

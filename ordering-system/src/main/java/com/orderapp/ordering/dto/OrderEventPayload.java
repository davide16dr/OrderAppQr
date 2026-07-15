package com.orderapp.ordering.dto;

public record OrderEventPayload(
    long orderId,
    long tenantId,
    long tenantSeq,
    String status,
    String eventType
) {
    public static OrderEventPayload created(long orderId, long tenantId, long tenantSeq) {
        return new OrderEventPayload(orderId, tenantId, tenantSeq, "NEW", "ORDER_CREATED");
    }

    public static OrderEventPayload statusChanged(long orderId, long tenantId, long tenantSeq, String status) {
        return new OrderEventPayload(orderId, tenantId, tenantSeq, status, "STATUS_CHANGED");
    }
}

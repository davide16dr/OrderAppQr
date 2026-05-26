package com.orderapp.ordering.service;

import com.orderapp.ordering.dto.OrderEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishOrderCreated(long orderId, long tenantId) {
        OrderEventPayload payload = OrderEventPayload.created(orderId, tenantId);
        messagingTemplate.convertAndSend("/topic/tenant/" + tenantId + "/orders", payload);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, payload);
    }

    public void publishStatusChanged(long orderId, long tenantId, String status) {
        OrderEventPayload payload = OrderEventPayload.statusChanged(orderId, tenantId, status);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, payload);
        messagingTemplate.convertAndSend("/topic/tenant/" + tenantId + "/orders", payload);
    }
}

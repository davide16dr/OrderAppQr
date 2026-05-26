package com.orderapp.ordering.controller;

import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.service.TenantSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant/subscriptions")
@RequiredArgsConstructor
public class TenantSubscriptionController {
    private final TenantSubscriptionService tenantSubscriptionService;

    @GetMapping("/current")
    public ResponseEntity<TenantSubscription> getCurrentSubscription(@RequestParam Long tenantId) {
        return tenantSubscriptionService.getCurrentSubscription(tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TenantSubscription>> getSubscriptionsByTenant(@RequestParam Long tenantId) {
        List<TenantSubscription> subscriptions = tenantSubscriptionService.getSubscriptionsByTenantId(tenantId);
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping
    public ResponseEntity<TenantSubscription> createSubscription(
            @RequestParam Long tenantId,
            @RequestParam String planCode,
            @RequestParam(defaultValue = "MONTHLY") String billingCycle) {
        TenantSubscription subscription = tenantSubscriptionService.createSubscription(tenantId, planCode, billingCycle);
        return ResponseEntity.status(201).body(subscription);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<TenantSubscription> activateSubscription(@PathVariable Long id) {
        TenantSubscription subscription = tenantSubscriptionService.activateSubscription(id);
        return ResponseEntity.ok(subscription);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<TenantSubscription> cancelSubscription(@PathVariable Long id) {
        TenantSubscription subscription = tenantSubscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(subscription);
    }
}

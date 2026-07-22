package com.orderapp.ordering.controller;

import com.orderapp.ordering.model.dto.TenantSubscriptionResponseDto;
import com.orderapp.ordering.service.TenantSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tenant/subscriptions")
@RequiredArgsConstructor
public class TenantSubscriptionController {

    private final TenantSubscriptionService tenantSubscriptionService;

    /** GET current subscription details */
    @GetMapping("/current")
    public ResponseEntity<TenantSubscriptionResponseDto> getCurrent(@RequestParam Long tenantId) {
        return tenantSubscriptionService.getCurrentSubscriptionDto(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Cancel subscription at end of current period */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestParam Long tenantId) {
        try {
            TenantSubscriptionResponseDto dto = tenantSubscriptionService.cancelAtPeriodEnd(tenantId);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Undo cancellation — keep subscription active */
    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivate(@RequestParam Long tenantId) {
        try {
            TenantSubscriptionResponseDto dto = tenantSubscriptionService.reactivate(tenantId);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Switch billing cycle (MONTHLY ↔ YEARLY) */
    @PostMapping("/change-billing")
    public ResponseEntity<?> changeBilling(
            @RequestParam Long tenantId,
            @RequestParam String billingCycle) {
        try {
            TenantSubscriptionResponseDto dto = tenantSubscriptionService.changeBillingCycle(tenantId, billingCycle);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Create a Stripe Checkout session for renewing an expired/trial subscription */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "MONTHLY") String billingCycle,
            @RequestParam String customerEmail) {
        try {
            String url = tenantSubscriptionService.createRenewalCheckoutSession(tenantId, billingCycle, customerEmail);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Create a Stripe Billing Portal session URL */
    @PostMapping("/portal-session")
    public ResponseEntity<?> portalSession(
            @RequestParam Long tenantId,
            @RequestParam String returnUrl) {
        try {
            String url = tenantSubscriptionService.createPortalSession(tenantId, returnUrl);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

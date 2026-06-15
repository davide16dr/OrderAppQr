package com.orderapp.ordering.controller;

import com.orderapp.ordering.service.StripeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class StripeController {

    private final StripeService stripeService;

    /**
     * Stripe webhook endpoint.
     * Reads the raw body via HttpServletRequest to preserve byte-exact payload
     * required for Stripe's HMAC-SHA256 signature verification.
     * Must be excluded from CSRF and auth filters.
     */
    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader
    ) throws IOException {
        byte[] bytes = request.getInputStream().readAllBytes();
        String rawPayload = new String(bytes, StandardCharsets.UTF_8);

        stripeService.handleWebhook(rawPayload, sigHeader);
        return ResponseEntity.ok(Map.of("received", true));
    }
}

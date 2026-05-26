package com.orderapp.ordering.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Health & Status Controller - No authentication required
 * Useful for monitoring and heartbeat checks
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "System", description = "System health and status endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "System Health Check")
    public ResponseEntity<Map<String, Object>> getHealth() {
        log.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", OffsetDateTime.now(),
            "version", "1.0.0"
        ));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness Check")
    public ResponseEntity<Map<String, String>> isReady() {
        log.debug("Readiness check requested");
        return ResponseEntity.ok(Map.of("ready", "true"));
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness Check")
    public ResponseEntity<Map<String, String>> isAlive() {
        log.debug("Liveness check requested");
        return ResponseEntity.ok(Map.of("alive", "true"));
    }
}

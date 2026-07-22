package com.orderapp.ordering.controller;

import com.orderapp.ordering.service.AdminTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.orderapp.ordering.model.dto.TenantSummaryDto;
import jakarta.validation.constraints.Min;

@Slf4j
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;

    @GetMapping
    public ResponseEntity<List<TenantSummaryDto>> getAllTenants(
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        log.info("Fetching all tenants - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<TenantSummaryDto> tenants = adminTenantService.getAllTenants(pageable);
        return ResponseEntity.ok(tenants.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTenant(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminTenantService.getTenantById(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("message", "Tenant not found"));
        }
    }

    @PostMapping("/{tenantId}/expire")
    public ResponseEntity<Map<String, String>> expireSubscription(@PathVariable Long tenantId) {
        try {
            adminTenantService.expireSubscription(tenantId);
            return ResponseEntity.ok(Map.of("message", "Abbonamento scaduto"));
        } catch (Exception e) {
            log.error("Error expiring subscription for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tenantId}/renew")
    public ResponseEntity<Map<String, String>> renewManually(
            @PathVariable Long tenantId,
            @RequestBody Map<String, String> payload) {
        String billingCycle = payload.getOrDefault("billingCycle", "MONTHLY");
        try {
            adminTenantService.renewManually(tenantId, billingCycle);
            return ResponseEntity.ok(Map.of("message", "Abbonamento rinnovato manualmente"));
        } catch (Exception e) {
            log.error("Error renewing tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{tenantId}/status")
    public ResponseEntity<Map<String, String>> updateTenantStatus(
            @PathVariable @Min(1) Long tenantId, 
            @RequestBody Map<String, Boolean> payload) {
        
        Boolean isEnabled = payload.get("enabled");
        if (isEnabled == null) {
            log.warn("Invalid payload for tenant status update - missing 'enabled' field");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required field: 'enabled'"));
        }
        
        try {
            log.info("Updating tenant status - tenantId: {}, enabled: {}", tenantId, isEnabled);
            adminTenantService.updateTenantStatus(tenantId, isEnabled);
            return ResponseEntity.ok(
                    Map.of("message", "Tenant status updated successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tenantId: {}", tenantId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating tenant status - tenantId: {}", tenantId, e);
            return ResponseEntity.notFound().build();
        }
    }
}

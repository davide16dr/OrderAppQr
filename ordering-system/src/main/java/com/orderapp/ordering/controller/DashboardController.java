package com.orderapp.ordering.controller;

import com.orderapp.ordering.dto.CreateTenantProductRequestDto;
import com.orderapp.ordering.dto.CreateTenantCategoryRequestDto;
import com.orderapp.ordering.dto.DashboardMetricsDto;
import com.orderapp.ordering.dto.StaffOrderDto;
import com.orderapp.ordering.dto.StaffProductDto;
import com.orderapp.ordering.dto.StaffProductDetailsDto;
import com.orderapp.ordering.dto.TenantCategoryDto;
import com.orderapp.ordering.dto.TenantSettingsDto;
import com.orderapp.ordering.dto.UpdateOrderStatusRequestDto;
import com.orderapp.ordering.dto.UpdateTenantCategoryRequestDto;
import com.orderapp.ordering.dto.UpdateTenantProductRequestDto;
import com.orderapp.ordering.dto.UpdateBrandingRequest;
import com.orderapp.ordering.dto.UpdateTenantSettingsRequestDto;
import com.orderapp.ordering.multitenant.TenantContext;
import com.orderapp.ordering.service.CategoryService;
import com.orderapp.ordering.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final CategoryService categoryService;

    public DashboardController(DashboardService dashboardService, CategoryService categoryService) {
        this.dashboardService = dashboardService;
        this.categoryService = categoryService;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getDashboardMetrics start tenantId={} principal={}",
            tenantId,
            SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null);

        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(tenantId);

        log.info("[dashboard-controller] getDashboardMetrics done tenantId={} totalOrdersToday={} topProducts={}",
            tenantId,
            metrics.getTotalOrdersToday(),
            metrics.getTopProducts() != null ? metrics.getTopProducts().size() : 0);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<List<StaffProductDto>> getTenantProducts() {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getTenantProducts start tenantId={}", tenantId);
        List<StaffProductDto> products = dashboardService.getTenantProducts(tenantId);
        log.info("[dashboard-controller] getTenantProducts done tenantId={} count={}", tenantId, products != null ? products.size() : 0);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<StaffProductDetailsDto> getTenantProductDetails(@PathVariable Long productId) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getTenantProductDetails start tenantId={} productId={}", tenantId, productId);
        StaffProductDetailsDto product = dashboardService.getTenantProductDetails(tenantId, productId);
        log.info("[dashboard-controller] getTenantProductDetails done tenantId={} productId={}", tenantId, productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<List<StaffOrderDto>> getTenantOrders() {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getTenantOrders start tenantId={}", tenantId);
        List<StaffOrderDto> orders = dashboardService.getTenantOrders(tenantId);
        log.info("[dashboard-controller] getTenantOrders done tenantId={} count={}", tenantId, orders != null ? orders.size() : 0);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<List<StaffOrderDto>> getAllTenantOrders(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getAllTenantOrders start tenantId={} limit={}", tenantId, limit);
        List<StaffOrderDto> orders = dashboardService.getAllTenantOrders(tenantId, limit);
        log.info("[dashboard-controller] getAllTenantOrders done tenantId={} count={}", tenantId, orders != null ? orders.size() : 0);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<TenantSettingsDto> getTenantSettings() {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getTenantSettings start tenantId={}", tenantId);
        TenantSettingsDto settings = dashboardService.getTenantSettings(tenantId);
        log.info("[dashboard-controller] getTenantSettings done tenantId={}", tenantId);
        return ResponseEntity.ok(settings);
    }

    @PatchMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<TenantSettingsDto> updateTenantSettings(@RequestBody UpdateTenantSettingsRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] updateTenantSettings start tenantId={}", tenantId);
        TenantSettingsDto settings = dashboardService.updateTenantSettings(tenantId, request);
        log.info("[dashboard-controller] updateTenantSettings done tenantId={}", tenantId);
        return ResponseEntity.ok(settings);
    }

    @PatchMapping("/branding")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<Void> updateTenantBranding(@RequestBody UpdateBrandingRequest request) {
        Long tenantId = TenantContext.getTenantId();
        dashboardService.updateTenantBranding(tenantId, request.logoDataUrl());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<StaffProductDto> createTenantProduct(@RequestBody CreateTenantProductRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(dashboardService.createTenantProduct(tenantId, request));
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<StaffProductDto> updateTenantProduct(
            @PathVariable Long productId,
            @RequestBody UpdateTenantProductRequestDto request
    ) {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(dashboardService.updateTenantProduct(tenantId, productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<Void> deleteTenantProduct(@PathVariable Long productId) {
        Long tenantId = TenantContext.getTenantId();
        dashboardService.disableTenantProduct(tenantId, productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<Void> updateTenantOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequestDto request
    ) {
        Long tenantId = TenantContext.getTenantId();
        dashboardService.updateTenantOrderStatus(tenantId, orderId, currentStaffUserId(), request.status());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<java.util.List<TenantCategoryDto>> getTenantCategories() {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getTenantCategories start tenantId={}", tenantId);
        java.util.List<TenantCategoryDto> categories = categoryService.getTenantCategories(tenantId);
        log.info("[dashboard-controller] getTenantCategories done tenantId={} count={}", tenantId, categories != null ? categories.size() : 0);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<TenantCategoryDto> getTenantCategory(@PathVariable Long categoryId) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] getTenantCategory start tenantId={} categoryId={}", tenantId, categoryId);
        TenantCategoryDto category = categoryService.getTenantCategoryById(tenantId, categoryId);
        log.info("[dashboard-controller] getTenantCategory done tenantId={} categoryId={}", tenantId, categoryId);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<TenantCategoryDto> createTenantCategory(@RequestBody CreateTenantCategoryRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] createTenantCategory start tenantId={}", tenantId);
        TenantCategoryDto category = categoryService.createTenantCategory(tenantId, request);
        log.info("[dashboard-controller] createTenantCategory done tenantId={} categoryId={}", tenantId, category.id());
        return ResponseEntity.status(201).body(category);
    }

    @PatchMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<TenantCategoryDto> updateTenantCategory(
            @PathVariable Long categoryId,
            @RequestBody UpdateTenantCategoryRequestDto request
    ) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] updateTenantCategory start tenantId={} categoryId={}", tenantId, categoryId);
        TenantCategoryDto category = categoryService.updateTenantCategory(tenantId, categoryId, request);
        log.info("[dashboard-controller] updateTenantCategory done tenantId={} categoryId={}", tenantId, categoryId);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
    public ResponseEntity<Void> deleteTenantCategory(@PathVariable Long categoryId) {
        Long tenantId = TenantContext.getTenantId();
        log.info("[dashboard-controller] deleteTenantCategory start tenantId={} categoryId={}", tenantId, categoryId);
        categoryService.deleteTenantCategory(tenantId, categoryId);
        log.info("[dashboard-controller] deleteTenantCategory done tenantId={} categoryId={}", tenantId, categoryId);
        return ResponseEntity.noContent().build();
    }

    private Long currentStaffUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        String principal = authentication.getPrincipal().toString();
        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

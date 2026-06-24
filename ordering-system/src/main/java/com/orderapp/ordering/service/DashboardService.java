package com.orderapp.ordering.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orderapp.ordering.dto.CreateTenantProductRequestDto;
import com.orderapp.ordering.dto.DashboardMetricsDto;
import com.orderapp.ordering.dto.StaffOrderDto;
import com.orderapp.ordering.dto.StaffProductDto;
import com.orderapp.ordering.dto.StaffProductDetailsDto;
import com.orderapp.ordering.dto.TenantSettingsDto;
import com.orderapp.ordering.dto.UpdateTenantProductRequestDto;
import com.orderapp.ordering.dto.UpdateTenantSettingsRequestDto;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.DashboardRepository;
import com.orderapp.ordering.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DashboardService {

        private static final Set<String> ALLOWED_TARGET_ORDER_STATUSES = Set.of("DELIVERED", "CANCELLED");
        private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
        private static final String DEFAULT_TIME_START = "00:00";
        private static final String DEFAULT_TIME_END = "23:59";

    private final DashboardRepository dashboardRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;
        private final OrderEventPublisher orderEventPublisher;

        public DashboardService(DashboardRepository dashboardRepository, TenantRepository tenantRepository, ObjectMapper objectMapper, OrderEventPublisher orderEventPublisher) {
        this.dashboardRepository = dashboardRepository;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
                this.orderEventPublisher = orderEventPublisher;
    }

    public DashboardMetricsDto getDashboardMetrics(Long tenantId) {
        DashboardMetricsDto metrics = new DashboardMetricsDto();

        // Metriche principali
        metrics.setTotalRevenueToday(dashboardRepository.getTotalRevenueToday(tenantId));
        metrics.setTotalOrdersToday(dashboardRepository.getTotalOrdersToday(tenantId));
        metrics.setActiveCustomerCount(dashboardRepository.getActiveCustomerCountToday(tenantId));
        metrics.setAverageOrderTime(dashboardRepository.getAverageOrderTime(tenantId));

        var ordersByHourData = dashboardRepository.getOrdersByHourToday(tenantId);
        var ordersByHour = ordersByHourData.stream()
                .map(row -> new DashboardMetricsDto.OrderByHourDto(
                        ((Number) row.get("hour")).intValue(),
                        ((Number) row.get("count")).intValue()
                ))
                .collect(Collectors.toList());
        metrics.setOrdersByHour(ordersByHour);

        var weeklyRevenueData = dashboardRepository.getWeeklyRevenue(tenantId);
        var weeklyRevenue = weeklyRevenueData.stream()
                .map(row -> new DashboardMetricsDto.WeeklyRevenueDto(
                        (String) row.get("day"),
                        ((Number) row.get("revenue")).doubleValue()
                ))
                .collect(Collectors.toList());
        metrics.setWeeklyRevenue(weeklyRevenue);

        var topProductsData = dashboardRepository.getTopProducts(tenantId);
        var topProducts = topProductsData.stream()
                .map(row -> new DashboardMetricsDto.TopProductDto(
                        ((Number) row.get("tenant_product_id")).longValue(),
                        (String) row.get("name"),
                        ((Number) row.get("quantity")).intValue()
                ))
                .collect(Collectors.toList());
        metrics.setTopProducts(topProducts);

        var areaDistributionData = dashboardRepository.getAreaDistribution(tenantId);
        var areaDistribution = areaDistributionData.stream()
                .map(row -> new DashboardMetricsDto.AreaDistributionDto(
                        row.get("id") != null ? ((Number) row.get("id")).longValue() : null,
                        (String) row.get("name"),
                        ((Number) row.get("order_count")).intValue()
                ))
                .collect(Collectors.toList());
        metrics.setAreaDistribution(areaDistribution);
        return metrics;
    }

        public List<StaffProductDto> getTenantProducts(Long tenantId) {
                return dashboardRepository.getTenantProducts(tenantId);
        }

        @Transactional(readOnly = true)
        public StaffProductDetailsDto getTenantProductDetails(Long tenantId, Long productId) {
                return dashboardRepository.getTenantProductDetails(tenantId, productId);
        }

        public List<StaffOrderDto> getTenantOrders(Long tenantId) {
                return dashboardRepository.getTenantOrders(tenantId);
        }

        public List<StaffOrderDto> getAllTenantOrders(Long tenantId, Integer limit) {
                int effectiveLimit = limit == null ? 300 : limit;
                return dashboardRepository.getAllTenantOrders(tenantId, effectiveLimit);
        }

        @Transactional(readOnly = true)
        public TenantSettingsDto getTenantSettings(Long tenantId) {
                Tenant tenant = tenantRepository.findById(tenantId)
                        .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

                JsonNode root = parseConfigOrEmpty(tenant.getOpeningConfigJson());

                JsonNode opening = root.path("openingHours");
                String openingTime = normalizeTimeOrDefault(opening.path("open").asText(null), DEFAULT_TIME_START);
                String closingTime = normalizeTimeOrDefault(opening.path("close").asText(null), DEFAULT_TIME_END);

                boolean orderingPaused = root.path("orderingPaused").asBoolean(false);

                JsonNode viewWindow = root.path("ordersViewWindow");
                String ordersViewStartTime = normalizeTimeOrDefault(viewWindow.path("start").asText(null), DEFAULT_TIME_START);
                String ordersViewEndTime = normalizeTimeOrDefault(viewWindow.path("end").asText(null), DEFAULT_TIME_END);

                return new TenantSettingsDto(openingTime, closingTime, orderingPaused, ordersViewStartTime, ordersViewEndTime);
        }

        @Transactional
        public TenantSettingsDto updateTenantSettings(Long tenantId, UpdateTenantSettingsRequestDto request) {
                Tenant tenant = tenantRepository.findById(tenantId)
                        .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

                String openingTime = trimToNull(request.openingTime());
                String closingTime = trimToNull(request.closingTime());
                String ordersViewStartTime = trimToNull(request.ordersViewStartTime());
                String ordersViewEndTime = trimToNull(request.ordersViewEndTime());

                validateTimeIfPresent(openingTime, "openingTime");
                validateTimeIfPresent(closingTime, "closingTime");
                validateTimeIfPresent(ordersViewStartTime, "ordersViewStartTime");
                validateTimeIfPresent(ordersViewEndTime, "ordersViewEndTime");

                JsonNode current = parseConfigOrEmpty(tenant.getOpeningConfigJson());
                ObjectNode root = current.isObject() ? (ObjectNode) current : objectMapper.createObjectNode();

                if (openingTime != null || closingTime != null) {
                        ObjectNode opening = root.withObject("openingHours");
                        if (openingTime != null) {
                                opening.put("open", openingTime);
                        }
                        if (closingTime != null) {
                                opening.put("close", closingTime);
                        }
                }

                if (request.orderingPaused() != null) {
                        root.put("orderingPaused", request.orderingPaused());
                }

                if (ordersViewStartTime != null || ordersViewEndTime != null) {
                        ObjectNode viewWindow = root.withObject("ordersViewWindow");
                        if (ordersViewStartTime != null) {
                                viewWindow.put("start", ordersViewStartTime);
                        }
                        if (ordersViewEndTime != null) {
                                viewWindow.put("end", ordersViewEndTime);
                        }
                }

                try {
                        tenant.setOpeningConfigJson(objectMapper.writeValueAsString(root));
                } catch (JsonProcessingException ex) {
                        throw new IllegalArgumentException("Invalid settings payload", ex);
                }

                tenantRepository.save(tenant);
                return getTenantSettings(tenantId);
        }

        @Transactional
        public void updateTenantBranding(Long tenantId, String logoDataUrl) {
                Tenant tenant = tenantRepository.findById(tenantId)
                                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

                if (logoDataUrl != null && logoDataUrl.length() > 3_000_000) {
                        throw new IllegalArgumentException("Logo troppo grande: massimo 2 MB");
                }

                try {
                        ObjectNode branding = objectMapper.createObjectNode();
                        String existing = tenant.getBrandingJson();
                        if (existing != null && !existing.isBlank()) {
                                JsonNode parsed = objectMapper.readTree(existing);
                                if (parsed.isObject()) {
                                        branding = (ObjectNode) parsed;
                                }
                        }
                        if (logoDataUrl != null && !logoDataUrl.isBlank()) {
                                branding.put("logoDataUrl", logoDataUrl);
                        } else {
                                branding.remove("logoDataUrl");
                        }
                        tenant.setBrandingJson(objectMapper.writeValueAsString(branding));
                        tenantRepository.save(tenant);
                } catch (JsonProcessingException ex) {
                        throw new IllegalArgumentException("Payload branding non valido", ex);
                }
        }

        private JsonNode parseConfigOrEmpty(String json) {
                if (json == null || json.isBlank()) {
                        return objectMapper.createObjectNode();
                }
                try {
                        JsonNode parsed = objectMapper.readTree(json);
                        return parsed != null ? parsed : objectMapper.createObjectNode();
                } catch (JsonProcessingException ex) {
                        return objectMapper.createObjectNode();
                }
        }

        private String normalizeTimeOrDefault(String value, String fallback) {
                String normalized = trimToNull(value);
                if (normalized == null || !TIME_PATTERN.matcher(normalized).matches()) {
                        return fallback;
                }
                return normalized;
        }

        private void validateTimeIfPresent(String value, String field) {
                if (value == null) {
                        return;
                }
                if (!TIME_PATTERN.matcher(value).matches()) {
                        throw new IllegalArgumentException("Invalid time format for " + field + ": " + value);
                }
        }

        private String trimToNull(String value) {
                if (value == null) {
                        return null;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        public StaffProductDto createTenantProduct(Long tenantId, CreateTenantProductRequestDto request) {
                if (request.getName() == null || request.getName().trim().isEmpty()) {
                        throw new IllegalArgumentException("Product name is required");
                }

                if (request.getPrice() == null || request.getPrice().signum() < 0) {
                        throw new IllegalArgumentException("Product price is required and must be >= 0");
                }

                if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
                        throw new IllegalArgumentException("Product category is required");
                }

                return dashboardRepository.createTenantProduct(tenantId, request);
        }

        public StaffProductDto updateTenantProduct(Long tenantId, Long productId, UpdateTenantProductRequestDto request) {
                if (request.getName() == null || request.getName().trim().isEmpty()) {
                        throw new IllegalArgumentException("Product name is required");
                }

                if (request.getPrice() == null || request.getPrice().signum() < 0) {
                        throw new IllegalArgumentException("Product price is required and must be >= 0");
                }

                if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
                        throw new IllegalArgumentException("Product category is required");
                }

                if (request.getVariants() != null) {
                        for (int i = 0; i < request.getVariants().size(); i++) {
                                var entry = request.getVariants().get(i);
                                if (entry == null || entry.getName() == null || entry.getName().trim().isEmpty()) {
                                        throw new IllegalArgumentException("Variant name is required at index " + i);
                                }
                                if (entry.getPriceDelta() == null) {
                                        throw new IllegalArgumentException("Variant priceDelta is required at index " + i);
                                }
                        }
                }

                if (request.getExtras() != null) {
                        for (int i = 0; i < request.getExtras().size(); i++) {
                                var entry = request.getExtras().get(i);
                                if (entry == null || entry.getName() == null || entry.getName().trim().isEmpty()) {
                                        throw new IllegalArgumentException("Extra name is required at index " + i);
                                }
                                if (entry.getPriceDelta() == null) {
                                        throw new IllegalArgumentException("Extra priceDelta is required at index " + i);
                                }
                        }
                }

                return dashboardRepository.updateTenantProduct(tenantId, productId, request);
        }

        public void disableTenantProduct(Long tenantId, Long productId) {
                boolean updated = dashboardRepository.disableTenantProduct(tenantId, productId);
                if (!updated) {
                        throw new IllegalArgumentException("Product not found for tenant");
                }
        }

        public void updateTenantOrderStatus(Long tenantId, Long orderId, Long staffUserId, String requestedStatus) {
                if (requestedStatus == null || requestedStatus.isBlank()) {
                        throw new IllegalArgumentException("Order status is required");
                }

                String newStatus = requestedStatus.trim().toUpperCase(Locale.ROOT);
                if (!ALLOWED_TARGET_ORDER_STATUSES.contains(newStatus)) {
                        throw new IllegalArgumentException("Unsupported target order status: " + requestedStatus);
                }

                String currentStatus = dashboardRepository.findTenantOrderStatus(tenantId, orderId)
                        .orElseThrow(() -> new IllegalArgumentException("Order not found for tenant"));

                if (currentStatus.equalsIgnoreCase(newStatus)) {
                        return;
                }

                boolean updated = dashboardRepository.updateTenantOrderStatus(tenantId, orderId, newStatus, staffUserId);
                if (!updated) {
                        throw new IllegalArgumentException("Unable to update order status");
                }

                orderEventPublisher.publishStatusChanged(orderId, tenantId, newStatus);
        }
}

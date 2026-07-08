
package com.orderapp.ordering.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orderapp.ordering.dto.CreateTenantProductRequestDto;
import com.orderapp.ordering.dto.StaffOrderDto;
import com.orderapp.ordering.dto.StaffOrderLineDto;
import com.orderapp.ordering.dto.StaffProductDto;
import com.orderapp.ordering.dto.StaffProductDetailsDto;
import com.orderapp.ordering.dto.UpdateTenantProductRequestDto;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Arrays;

@Repository
public class DashboardRepository {

        private final JdbcTemplate jdbcTemplate;
        private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
        private final ObjectMapper objectMapper;
        private final boolean isLocalProfile;

        public DashboardRepository(
                JdbcTemplate jdbcTemplate,
                NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                ObjectMapper objectMapper,
                Environment environment
        ) {
                this.jdbcTemplate = jdbcTemplate;
                this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
                this.objectMapper = objectMapper;
                String datasourceUrl = environment.getProperty("spring.datasource.url", "");
                boolean hasLocalProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");
                this.isLocalProfile = hasLocalProfile || datasourceUrl.startsWith("jdbc:h2:");
        }
        public BigDecimal getTotalRevenueToday(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT COALESCE(SUM(o.total_amount), 0) as total
                        FROM orders o
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                          AND o.status <> 'CANCELLED'
                        """
                        : """
                        SELECT COALESCE(SUM(o.total_amount), 0) as total
                        FROM orders o
                        WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE AND o.status != 'CANCELLED'
                        """;
                return jdbcTemplate.queryForObject(sql, BigDecimal.class, tenantId);
        }

        public Integer getTotalOrdersToday(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT COUNT(*) as total
                        FROM orders o
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                          AND o.status <> 'CANCELLED'
                        """
                        : """
                        SELECT COUNT(*) as total FROM orders o
                        WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE AND o.status != 'CANCELLED'
                        """;
                return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
        }

        public Integer getActiveCustomerCountToday(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT COUNT(DISTINCT o.location_id) as count
                        FROM orders o
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                        """
                        : """
                        SELECT COUNT(DISTINCT o.location_id) as count FROM orders o
                        WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE
                        """;
                return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
        }

        public Double getAverageOrderTime(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT COALESCE(AVG(DATEDIFF('MINUTE', o.created_at, o.delivered_at)), 0) as avg_time
                        FROM orders o
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                          AND o.delivered_at IS NOT NULL
                          AND o.status = 'DELIVERED'
                        """
                        : """
                        SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (o.delivered_at - o.created_at)) / 60), 0) as avg_time
                        FROM orders o WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE
                        AND o.delivered_at IS NOT NULL AND o.status = 'DELIVERED'
                        """;
                return jdbcTemplate.queryForObject(sql, Double.class, tenantId);
        }

        public List<Map<String, Object>> getOrdersByHourToday(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT HOUR(o.created_at) AS "hour", COUNT(*) AS "count"
                        FROM orders o
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                        GROUP BY HOUR(o.created_at)
                        ORDER BY "hour"
                        """
                        : """
                        SELECT EXTRACT(HOUR FROM o.created_at)::INTEGER as hour, COUNT(*) as count
                        FROM orders o WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE
                        GROUP BY EXTRACT(HOUR FROM o.created_at) ORDER BY hour
                        """;
                return jdbcTemplate.queryForList(sql, tenantId);
        }

        public List<Map<String, Object>> getWeeklyRevenue(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                                                                                                SELECT
                                                                                                        CASE DAY_OF_WEEK(x.d)
                                                                                                                WHEN 1 THEN 'Dom' WHEN 2 THEN 'Lun' WHEN 3 THEN 'Mar' WHEN 4 THEN 'Mer'
                                                                                                                WHEN 5 THEN 'Gio' WHEN 6 THEN 'Ven' WHEN 7 THEN 'Sab'
                                                                                                        END AS "day",
                                                                                                        COALESCE(SUM(x.total_amount), 0) AS "revenue"
                                                                                                FROM (
                                                                                                        SELECT CAST(o.created_at AS DATE) AS d, o.total_amount
                                                                                                        FROM orders o
                                                                                                        WHERE o.tenant_id = ?
                                                                                                                AND o.created_at >= DATEADD('DAY', -7, CURRENT_DATE)
                                                                                                                AND o.status <> 'CANCELLED'
                                                                                                ) x
                                                                                                GROUP BY x.d
                                                                                                ORDER BY x.d
                        """
                        : """
                        SELECT CASE EXTRACT(DOW FROM DATE(o.created_at))
                                WHEN 0 THEN 'Dom' WHEN 1 THEN 'Lun' WHEN 2 THEN 'Mar' WHEN 3 THEN 'Mer'
                                WHEN 4 THEN 'Gio' WHEN 5 THEN 'Ven' WHEN 6 THEN 'Sab'
                        END as day, COALESCE(SUM(o.total_amount), 0) as revenue
                        FROM orders o WHERE o.tenant_id = ? AND o.created_at >= CURRENT_DATE - INTERVAL '7 days'
                        AND o.status != 'CANCELLED' GROUP BY DATE(o.created_at) ORDER BY DATE(o.created_at)
                        """;
                return jdbcTemplate.queryForList(sql, tenantId);
        }

        public List<Map<String, Object>> getTopProducts(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT oi.tenant_product_id, tp.name, COUNT(*) as quantity
                        FROM order_items oi
                        JOIN orders o ON oi.order_id = o.id
                        JOIN tenant_products tp ON oi.tenant_product_id = tp.id
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                          AND o.status <> 'CANCELLED'
                        GROUP BY oi.tenant_product_id, tp.name
                        ORDER BY quantity DESC
                        LIMIT 5
                        """
                        : """
                        SELECT oi.tenant_product_id, tp.name, COUNT(*) as quantity FROM order_items oi
                        JOIN orders o ON oi.order_id = o.id JOIN tenant_products tp ON oi.tenant_product_id = tp.id
                        WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE AND o.status != 'CANCELLED'
                        GROUP BY oi.tenant_product_id, tp.name ORDER BY quantity DESC LIMIT 5
                        """;
                return jdbcTemplate.queryForList(sql, tenantId);
        }

        public List<Map<String, Object>> getAreaDistribution(Long tenantId) {
                String sql = isLocalProfile
                        ? """
                        SELECT a.id, a.name, COUNT(o.id) as order_count
                        FROM orders o
                        LEFT JOIN areas a ON o.location_id = (
                                SELECT id
                                FROM locations
                                WHERE area_id = a.id AND tenant_id = o.tenant_id
                                LIMIT 1
                        )
                        WHERE o.tenant_id = ?
                          AND CAST(o.created_at AS DATE) = CURRENT_DATE
                        GROUP BY a.id, a.name
                        ORDER BY order_count DESC
                        """
                        : """
                        SELECT a.id, a.name, COUNT(o.id) as order_count FROM orders o
                        LEFT JOIN areas a ON o.location_id = (
                                SELECT id FROM locations WHERE area_id = a.id AND tenant_id = o.tenant_id LIMIT 1
                        ) WHERE o.tenant_id = ? AND DATE(o.created_at) = CURRENT_DATE
                        GROUP BY a.id, a.name ORDER BY order_count DESC
                        """;
                return jdbcTemplate.queryForList(sql, tenantId);
        }

        public List<StaffProductDto> getTenantProducts(Long tenantId) {
                                                                String sql = isLocalProfile
                                                                                                ? """
                                                                                                SELECT
                                                                                                                tp.id,
                                                                                                                tp.name,
                                                                                                                tp.description,
                                                                                                                tp.price,
                                                                                                                tp.image_url,
                                                                                                                COALESCE((
                                                                                                                                SELECT c.name
                                                                                                                                FROM category_tenant_products ctp
                                                                                                                                JOIN categories c ON c.id = ctp.category_id
                                                                                                                                WHERE ctp.tenant_product_id = tp.id
                                                                                                                                        AND c.tenant_id = tp.tenant_id
                                                                                                                                ORDER BY ctp.display_order, c.display_order, c.id
                                                                                                                                LIMIT 1
                                                                                                                ), 'Senza categoria') AS category,
                                                                                                                tp.department,
                                                                                                                tp.vat_rate,
                                                                                                                tp.status,
                                                                                                                tp.available_for_order,
                                                                                                                0 AS variants_count,
                                                                                                                0 AS extras_count,
                                                                                                                tp.sku
                                                                                                FROM tenant_products tp
                                                                                                WHERE tp.tenant_id = ?
                                                                                                  AND COALESCE(tp.status, 'ACTIVE') <> 'DISABLED'
                                                                                                ORDER BY tp.created_at DESC, tp.id DESC
                                                                                                """
                                                                                                : """
                                                                                                SELECT
                                                                                                                tp.id,
                                                                                                                tp.name,
                                                                                                                tp.description,
                                                                                                                tp.price,
                                                                                                                tp.image_url,
                                                                                                                COALESCE(c.name, 'Senza categoria') AS category,
                                                                                                                tp.department,
                                                                                                                tp.vat_rate,
                                                                                                                tp.status,
                                                                                                                tp.available_for_order,
                                                                                                                COALESCE(jsonb_array_length(tp.metadata_json -> 'variants'), 0) AS variants_count,
                                                                                                                COALESCE(jsonb_array_length(tp.metadata_json -> 'extras'), 0) AS extras_count,
                                                                                                                tp.sku
                                                                                                FROM tenant_products tp
                                                                                                LEFT JOIN LATERAL (
                                                                                                                SELECT c.name
                                                                                                                FROM category_tenant_products ctp
                                                                                                                JOIN categories c ON c.id = ctp.category_id
                                                                                                                WHERE ctp.tenant_product_id = tp.id
                                                                                                                        AND c.tenant_id = tp.tenant_id
                                                                                                                ORDER BY ctp.display_order, c.display_order, c.id
                                                                                                                LIMIT 1
                                                                                                ) c ON true
                                                                                                WHERE tp.tenant_id = ?
                                                                                                  AND COALESCE(tp.status, 'ACTIVE') <> 'DISABLED'
                                                                                                ORDER BY tp.created_at DESC, tp.id DESC
                                                                                                """;

                return jdbcTemplate.query(sql, (rs, rowNum) -> new StaffProductDto(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getString("image_url"),
                        rs.getString("category"),
                        rs.getString("department"),
                        rs.getBigDecimal("vat_rate"),
                        rs.getString("status"),
                        rs.getBoolean("available_for_order"),
                        rs.getInt("variants_count"),
                        rs.getInt("extras_count"),
                        rs.getString("sku")
                ), tenantId);
        }

        public StaffProductDetailsDto getTenantProductDetails(Long tenantId, Long productId) {
                String sql = isLocalProfile
                        ? """
                        SELECT
                                tp.id,
                                tp.name,
                                tp.description,
                                tp.price,
                                tp.image_url,
                                COALESCE((
                                        SELECT c.name
                                        FROM category_tenant_products ctp
                                        JOIN categories c ON c.id = ctp.category_id
                                        WHERE ctp.tenant_product_id = tp.id
                                          AND c.tenant_id = tp.tenant_id
                                        ORDER BY ctp.display_order, c.display_order, c.id
                                        LIMIT 1
                                ), 'Senza categoria') AS category,
                                tp.department,
                                tp.vat_rate,
                                tp.status,
                                tp.available_for_order,
                                tp.sku,
                                tp.metadata_json
                        FROM tenant_products tp
                        WHERE tp.tenant_id = ?
                          AND tp.id = ?
                          AND COALESCE(tp.status, 'ACTIVE') <> 'DISABLED'
                        LIMIT 1
                        """
                        : """
                        SELECT
                                tp.id,
                                tp.name,
                                tp.description,
                                tp.price,
                                tp.image_url,
                                COALESCE(c.name, 'Senza categoria') AS category,
                                tp.department,
                                tp.vat_rate,
                                tp.status,
                                tp.available_for_order,
                                tp.sku,
                                tp.metadata_json
                        FROM tenant_products tp
                        LEFT JOIN LATERAL (
                                SELECT c.name
                                FROM category_tenant_products ctp
                                JOIN categories c ON c.id = ctp.category_id
                                WHERE ctp.tenant_product_id = tp.id
                                  AND c.tenant_id = tp.tenant_id
                                ORDER BY ctp.display_order, c.display_order, c.id
                                LIMIT 1
                        ) c ON true
                        WHERE tp.tenant_id = ?
                          AND tp.id = ?
                          AND COALESCE(tp.status, 'ACTIVE') <> 'DISABLED'
                        LIMIT 1
                        """;

                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId, productId);
                if (rows.isEmpty()) {
                        throw new IllegalArgumentException("Product not found for tenant");
                }

                Map<String, Object> row = rows.get(0);
                Object metadataObj = row.get("metadata_json");

                List<CreateTenantProductRequestDto.VariantEntry> variants = List.of();
                List<CreateTenantProductRequestDto.VariantEntry> extras = List.of();

                if (metadataObj != null) {
                        try {
                                JsonNode json = objectMapper.readTree(metadataObj.toString());

                                JsonNode variantsNode = firstPresentField(
                                        json,
                                        "variants",
                                        "variantEntries",
                                        "variant_entries",
                                        "variantOptions",
                                        "variant_options",
                                        "variantsJson",
                                        "variants_json"
                                );
                                JsonNode extrasNode = firstPresentField(
                                        json,
                                        "extras",
                                        "extraEntries",
                                        "extra_entries",
                                        "extraOptions",
                                        "extra_options",
                                        "extrasJson",
                                        "extras_json",
                                        "addons",
                                        "addOns"
                                );

                                variants = parseVariantEntries(variantsNode);
                                extras = parseVariantEntries(extrasNode);
                        } catch (Exception ignored) {
                                variants = List.of();
                                extras = List.of();
                        }
                }

                int variantsCount = variants == null ? 0 : variants.size();
                int extrasCount = extras == null ? 0 : extras.size();

                return new StaffProductDetailsDto(
                        ((Number) row.get("id")).longValue(),
                        (String) row.get("name"),
                        (String) row.get("description"),
                        (BigDecimal) row.get("price"),
                        (String) row.get("image_url"),
                        (String) row.get("category"),
                        (String) row.get("department"),
                        row.get("vat_rate") instanceof BigDecimal bd ? bd : BigDecimal.valueOf(10),
                        (String) row.get("status"),
                        row.get("available_for_order") instanceof Boolean b ? b : true,
                        variantsCount,
                        extrasCount,
                        (String) row.get("sku"),
                        variants,
                        extras
                );
        }

        private JsonNode firstPresentField(JsonNode root, String... names) {
                if (root == null || root.isNull() || !root.isObject() || names == null) {
                        return null;
                }

                for (String name : names) {
                        if (name == null || name.isBlank()) {
                                continue;
                        }
                        JsonNode value = root.get(name);
                        if (value != null && !value.isNull() && !value.isMissingNode()) {
                                return value;
                        }
                }

                return null;
        }

        private List<CreateTenantProductRequestDto.VariantEntry> parseVariantEntries(JsonNode node) {
                JsonNode normalized = normalizeVariantContainer(node);
                if (normalized == null || normalized.isNull() || normalized.isMissingNode()) {
                        return List.of();
                }

                List<CreateTenantProductRequestDto.VariantEntry> result = new ArrayList<>();

                if (normalized.isArray()) {
                        for (JsonNode item : normalized) {
                                appendVariantEntry(result, item);
                        }
                        return result;
                }

                if (normalized.isObject()) {
                        // Legacy: variants/extras as map: {"Small": 0, "Large": 1.5}
                        normalized.fields().forEachRemaining(entry -> {
                                String name = entry.getKey();
                                BigDecimal priceDelta = parseBigDecimal(entry.getValue(), BigDecimal.ZERO);
                                addVariantEntry(result, name, priceDelta);
                        });
                        return result;
                }

                if (normalized.isTextual()) {
                        // Legacy: comma-separated string
                        String raw = normalized.asText("").trim();
                        if (!raw.isEmpty()) {
                                for (String part : raw.split("[,;]")) {
                                        addVariantEntry(result, part, BigDecimal.ZERO);
                                }
                        }
                        return result;
                }

                // Fallback: a single scalar value
                addVariantEntry(result, normalized.asText(null), BigDecimal.ZERO);
                return result;
        }

        private JsonNode normalizeVariantContainer(JsonNode node) {
                if (node == null || node.isNull() || node.isMissingNode()) {
                        return null;
                }

                // Common legacy: variants/extras stored as JSON string
                if (node.isTextual()) {
                        String raw = node.asText("");
                        String trimmed = raw == null ? "" : raw.trim();
                        if (!trimmed.isEmpty() && (trimmed.startsWith("[") || trimmed.startsWith("{"))) {
                                try {
                                        return objectMapper.readTree(trimmed);
                                } catch (Exception ignored) {
                                        return node;
                                }
                        }
                        return node;
                }

                // Common legacy: wrappers { items: [...] } / { options: [...] }
                if (node.isObject()) {
                        JsonNode items = firstPresentField(node, "items", "options", "values", "entries", "data");
                        if (items != null) {
                                return items;
                        }
                }

                return node;
        }

        private void appendVariantEntry(List<CreateTenantProductRequestDto.VariantEntry> target, JsonNode item) {
                if (item == null || item.isNull() || item.isMissingNode()) {
                        return;
                }

                // Sometimes an entry is itself a JSON string
                if (item.isTextual()) {
                        String raw = item.asText(null);
                        if (raw == null) {
                                return;
                        }
                        String trimmed = raw.trim();
                        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                                try {
                                        JsonNode parsed = objectMapper.readTree(trimmed);
                                        if (parsed != null) {
                                                if (parsed.isArray()) {
                                                        for (JsonNode nested : parsed) {
                                                                appendVariantEntry(target, nested);
                                                        }
                                                        return;
                                                }
                                                item = parsed;
                                        }
                                } catch (Exception ignored) {
                                        // fall through
                                }
                        }

                        addVariantEntry(target, trimmed, BigDecimal.ZERO);
                        return;
                }

                if (item.isArray()) {
                        // Legacy tuple: ["Small", 0.5]
                        String name = item.size() > 0 ? item.get(0).asText(null) : null;
                        BigDecimal priceDelta = item.size() > 1 ? parseBigDecimal(item.get(1), BigDecimal.ZERO) : BigDecimal.ZERO;
                        addVariantEntry(target, name, priceDelta);
                        return;
                }

                if (!item.isObject()) {
                        addVariantEntry(target, item.asText(null), BigDecimal.ZERO);
                        return;
                }

                String name = firstNonBlankText(item, "name", "label", "title", "text", "value");

                JsonNode deltaNode = firstPresentField(
                        item,
                        "priceDelta",
                        "price_delta",
                        "delta",
                        "amount",
                        "price",
                        "priceDeltaCents",
                        "price_delta_cents"
                );

                BigDecimal priceDelta = parseBigDecimal(deltaNode, BigDecimal.ZERO);
                addVariantEntry(target, name, priceDelta);
        }

        private String firstNonBlankText(JsonNode obj, String... keys) {
                JsonNode value = firstPresentField(obj, keys);
                if (value == null || value.isNull() || value.isMissingNode()) {
                        return null;
                }
                String text = value.asText(null);
                if (text == null) {
                        return null;
                }
                String trimmed = text.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        private BigDecimal parseBigDecimal(JsonNode node, BigDecimal fallback) {
                if (node == null || node.isNull() || node.isMissingNode()) {
                        return fallback;
                }

                try {
                        if (node.isNumber()) {
                                return node.decimalValue();
                        }
                        String text = node.asText(null);
                        if (text == null) {
                                return fallback;
                        }
                        String trimmed = text.trim();
                        return trimmed.isEmpty() ? fallback : new BigDecimal(trimmed);
                } catch (Exception ignored) {
                        return fallback;
                }
        }

        private void addVariantEntry(
                List<CreateTenantProductRequestDto.VariantEntry> target,
                String name,
                BigDecimal priceDelta
        ) {
                if (target == null) {
                        return;
                }
                if (name == null) {
                        return;
                }

                String trimmed = name.trim();
                if (trimmed.isEmpty()) {
                        return;
                }

                CreateTenantProductRequestDto.VariantEntry entry = new CreateTenantProductRequestDto.VariantEntry();
                entry.setName(trimmed);
                entry.setPriceDelta(priceDelta == null ? BigDecimal.ZERO : priceDelta);
                target.add(entry);
        }

        public List<StaffOrderDto> getTenantOrders(Long tenantId) {
                return getTenantOrdersInternal(tenantId, false, 80);
        }

        public List<StaffOrderDto> getAllTenantOrders(Long tenantId, int limit) {
                int effectiveLimit = Math.max(1, Math.min(limit, 2000));
                return getTenantOrdersInternal(tenantId, true, effectiveLimit);
        }

        private List<StaffOrderDto> getTenantOrdersInternal(Long tenantId, boolean includeCancelled, int limit) {
                String cancelledFilter = includeCancelled ? "" : "  AND o.status <> 'CANCELLED'\n";

                String headersSql = isLocalProfile
                        ? """
                        SELECT
                                o.id,
                                ('#D' || RIGHT('000' || CAST(o.id AS varchar(20)), 3)) AS code,
                                o.location_label_snapshot AS location_label,
                                COALESCE(o.area_name_snapshot, 'Senza area') AS area_name,
                                CASE
                                        WHEN EXISTS (
                                                SELECT 1
                                                FROM order_items oi
                                                WHERE oi.order_id = o.id AND oi.department_snapshot = 'KITCHEN'
                                        ) THEN 'KITCHEN'
                                        ELSE 'BAR'
                                END AS type,
                                o.status,
                                o.customer_note,
                                o.total_amount,
                                o.created_at
                        FROM orders o
                        WHERE o.tenant_id = ?
                        %s                        ORDER BY o.created_at DESC
                        LIMIT %d
                        """.formatted(cancelledFilter, limit)
                        : """
                        SELECT
                                o.id,
                                ('#D' || RIGHT('000' || o.id::text, 3)) AS code,
                                o.location_label_snapshot AS location_label,
                                COALESCE(o.area_name_snapshot, 'Senza area') AS area_name,
                                CASE
                                        WHEN EXISTS (
                                                SELECT 1
                                                FROM order_items oi
                                                WHERE oi.order_id = o.id AND oi.department_snapshot = 'KITCHEN'
                                        ) THEN 'KITCHEN'
                                        ELSE 'BAR'
                                END AS type,
                                o.status,
                                o.customer_note,
                                o.total_amount,
                                o.created_at
                        FROM orders o
                        WHERE o.tenant_id = ?
                        %s                        ORDER BY o.created_at DESC
                        LIMIT %d
                        """.formatted(cancelledFilter, limit);

                List<Map<String, Object>> headerRows = jdbcTemplate.queryForList(headersSql, tenantId);
                if (headerRows.isEmpty()) {
                        return List.of();
                }

                List<Long> orderIds = headerRows.stream()
                        .map(row -> ((Number) row.get("id")).longValue())
                        .toList();

                String itemsSql = """
                        SELECT
                            oi.order_id,
                            oi.quantity,
                            oi.product_name_snapshot,
                            oi.line_total,
                            oi.id,
                            STRING_AGG(
                                CASE WHEN oimo.modifier_group_name_snapshot IS NOT NULL
                                     THEN oimo.modifier_group_name_snapshot || ': ' || oimo.option_name_snapshot
                                     ELSE NULL
                                END,
                                ', '
                                ORDER BY oimo.modifier_group_name_snapshot, oimo.option_name_snapshot
                            ) as variant_details
                        FROM order_items oi
                        LEFT JOIN order_item_modifier_options oimo ON oi.id = oimo.order_item_id
                        WHERE oi.tenant_id = :tenantId
                          AND oi.order_id IN (:orderIds)
                        GROUP BY oi.order_id, oi.id, oi.quantity, oi.product_name_snapshot, oi.line_total
                        ORDER BY oi.order_id, oi.id
                        """;

                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("tenantId", tenantId)
                        .addValue("orderIds", orderIds);

                List<Map<String, Object>> itemRows = namedParameterJdbcTemplate.queryForList(itemsSql, params);
                Map<Long, List<StaffOrderLineDto>> itemsByOrder = new HashMap<>();

                for (Map<String, Object> itemRow : itemRows) {
                        Long orderId = ((Number) itemRow.get("order_id")).longValue();
                        String variantDetails = (String) itemRow.get("variant_details");
                        if (variantDetails == null || variantDetails.isBlank()) {
                                String pname = (String) itemRow.get("product_name_snapshot");
                                variantDetails = extractSuffixFromName(pname);
                        }
                        StaffOrderLineDto line = new StaffOrderLineDto(
                                ((Number) itemRow.get("quantity")).intValue(),
                                (String) itemRow.get("product_name_snapshot"),
                                (BigDecimal) itemRow.get("line_total"),
                                variantDetails
                        );
                        itemsByOrder.computeIfAbsent(orderId, key -> new ArrayList<>()).add(line);
                }

                List<StaffOrderDto> result = new ArrayList<>();
                for (Map<String, Object> row : headerRows) {
                        Long id = ((Number) row.get("id")).longValue();
                        result.add(new StaffOrderDto(
                                id,
                                (String) row.get("code"),
                                (String) row.get("location_label"),
                                (String) row.get("area_name"),
                                (String) row.get("type"),
                                (String) row.get("status"),
                                (String) row.get("customer_note"),
                                (BigDecimal) row.get("total_amount"),
                                toOffsetDateTime(row.get("created_at")),
                                itemsByOrder.getOrDefault(id, List.of())
                        ));
                }

                return result;
        }

        private static String extractSuffixFromName(String name) {
                if (name == null) return null;
                Matcher m = Pattern.compile("\\(([^)]+)\\)\\s*$").matcher(name);
                if (m.find()) {
                        String inside = m.group(1);
                        return inside == null ? null : inside.trim();
                }
                return null;
        }

        private OffsetDateTime toOffsetDateTime(Object value) {
                if (value == null) {
                        return null;
                }
                if (value instanceof OffsetDateTime odt) {
                        return odt;
                }
                if (value instanceof Timestamp ts) {
                        return ts.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                }
                if (value instanceof LocalDateTime ldt) {
                        return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
                }
                if (value instanceof java.util.Date d) {
                        return d.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
                }
                throw new IllegalArgumentException("Unsupported datetime value: " + value.getClass());
        }

        public StaffProductDto createTenantProduct(Long tenantId, CreateTenantProductRequestDto request) {
                String category = request.getCategory() == null ? "" : request.getCategory().trim();
                String name = request.getName() == null ? "" : request.getName().trim();
                String description = request.getDescription() == null ? null : request.getDescription().trim();
                boolean availableForOrder = request.getAvailableForOrder() == null || request.getAvailableForOrder();

                String department = inferDepartment(category);
                BigDecimal vatRate = BigDecimal.valueOf(10);
                String sku = generateSku(name);

                Map<String, Object> metadata = Map.of(
                        "variants", Objects.requireNonNullElse(request.getVariants(), List.of()),
                        "extras", Objects.requireNonNullElse(request.getExtras(), List.of())
                );

                String metadataJson;
                try {
                        metadataJson = objectMapper.writeValueAsString(metadata);
                } catch (JsonProcessingException ex) {
                        throw new IllegalArgumentException("Invalid metadata payload", ex);
                }

                String categorySelectSql = """
                        SELECT id
                        FROM categories
                        WHERE tenant_id = ? AND lower(name) = lower(?)
                        LIMIT 1
                        """;

                List<Long> existingCategory = jdbcTemplate.query(categorySelectSql,
                        (rs, rowNum) -> rs.getLong("id"), tenantId, category);

                Long categoryId;
                if (existingCategory.isEmpty()) {
                        if (isLocalProfile) {
                                String categoryInsertSql = """
                                INSERT INTO categories (tenant_id, name, status, display_order, created_at, updated_at)
                                VALUES (?, ?, 'ACTIVE', 0, now(), now())
                                """;
                                jdbcTemplate.update(categoryInsertSql, tenantId, category);
                                categoryId = jdbcTemplate.queryForObject(categorySelectSql, Long.class, tenantId, category);
                        } else {
                                String categoryInsertSql = """
                                INSERT INTO categories (tenant_id, name, status, display_order, created_at, updated_at)
                                VALUES (?, ?, 'ACTIVE', 0, now(), now())
                                RETURNING id
                                """;
                                categoryId = jdbcTemplate.queryForObject(categoryInsertSql, Long.class, tenantId, category);
                        }
                } else {
                        categoryId = existingCategory.get(0);
                }

                Long productId;
                if (isLocalProfile) {
                        String productInsertSql = """
                        INSERT INTO tenant_products (
                            tenant_id,
                            sku,
                            name,
                            description,
                            price,
                            image_url,
                            department,
                            vat_rate,
                            status,
                            available_for_order,
                            is_customized,
                            metadata_json,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, true, ?, now(), now())
                        """;

                        KeyHolder keyHolder = new GeneratedKeyHolder();
                        jdbcTemplate.update(connection -> {
                                var ps = connection.prepareStatement(productInsertSql, new String[]{"id"});
                                ps.setLong(1, tenantId);
                                ps.setString(2, sku);
                                ps.setString(3, name);
                                ps.setString(4, description);
                                ps.setBigDecimal(5, request.getPrice());
                                ps.setString(6, request.getImageDataUrl());
                                ps.setString(7, department);
                                ps.setBigDecimal(8, vatRate);
                                ps.setBoolean(9, availableForOrder);
                                ps.setString(10, metadataJson);
                                return ps;
                        }, keyHolder);

                        Number key = keyHolder.getKey();
                        if (key == null) {
                                throw new IllegalStateException("Unable to retrieve generated product id");
                        }
                        productId = key.longValue();

                        String linkSql = """
                        MERGE INTO category_tenant_products (category_id, tenant_product_id, display_order)
                        KEY (category_id, tenant_product_id)
                        VALUES (?, ?, 0)
                        """;
                        jdbcTemplate.update(linkSql, categoryId, productId);
                } else {
                        String productInsertSql = """
                        INSERT INTO tenant_products (
                            tenant_id,
                            sku,
                            name,
                            description,
                            price,
                            image_url,
                            department,
                            vat_rate,
                            status,
                            available_for_order,
                            is_customized,
                            metadata_json,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, true, ?::jsonb, now(), now())
                        RETURNING id
                        """;

                        productId = jdbcTemplate.queryForObject(
                                productInsertSql,
                                Long.class,
                                tenantId,
                                sku,
                                name,
                                description,
                                request.getPrice(),
                                request.getImageDataUrl(),
                                department,
                                vatRate,
                                availableForOrder,
                                metadataJson
                        );

                        String linkSql = """
                        INSERT INTO category_tenant_products (category_id, tenant_product_id, display_order)
                        VALUES (?, ?, 0)
                        ON CONFLICT DO NOTHING
                        """;
                        jdbcTemplate.update(linkSql, categoryId, productId);
                }

                Integer variantsCount = request.getVariants() == null ? 0 : request.getVariants().size();
                Integer extrasCount = request.getExtras() == null ? 0 : request.getExtras().size();

                return new StaffProductDto(
                        productId,
                        name,
                        description,
                        request.getPrice(),
                        request.getImageDataUrl(),
                        category,
                        department,
                        vatRate,
                        "ACTIVE",
                        availableForOrder,
                        variantsCount,
                        extrasCount,
                        sku
                );
        }

        public StaffProductDto updateTenantProduct(Long tenantId, Long productId, UpdateTenantProductRequestDto request) {
                String category = request.getCategory() == null ? "" : request.getCategory().trim();
                String name = request.getName() == null ? "" : request.getName().trim();
                String description = request.getDescription() == null ? null : request.getDescription().trim();

                String department = inferDepartment(category);

                // Recupero i campi che non vogliamo ricalcolare/perdere.
                String selectExistingSql = """
                        SELECT sku, vat_rate, status, metadata_json, image_url, available_for_order
                        FROM tenant_products
                        WHERE tenant_id = ?
                          AND id = ?
                          AND COALESCE(status, 'ACTIVE') <> 'DISABLED'
                        LIMIT 1
                        """;

                List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(selectExistingSql, tenantId, productId);
                if (existingRows.isEmpty()) {
                        throw new IllegalArgumentException("Product not found for tenant");
                }

                Map<String, Object> existing = existingRows.get(0);
                String sku = (String) existing.get("sku");
                BigDecimal vatRate = existing.get("vat_rate") instanceof BigDecimal bd ? bd : BigDecimal.valueOf(10);
                String status = (String) existing.get("status");
                String existingImageUrl = existing.get("image_url") == null ? null : existing.get("image_url").toString();
                boolean existingAvailableForOrder = existing.get("available_for_order") instanceof Boolean b ? b : true;

                boolean resolvedAvailableForOrder = request.getAvailableForOrder() == null ? existingAvailableForOrder : request.getAvailableForOrder();
                String resolvedImageUrl = request.getImageDataUrl() == null ? existingImageUrl : request.getImageDataUrl();

                Object metadataObj = existing.get("metadata_json");

                ObjectNode metadataRoot = objectMapper.createObjectNode();
                if (metadataObj != null) {
                        try {
                                JsonNode parsed = objectMapper.readTree(metadataObj.toString());
                                if (parsed != null && parsed.isObject()) {
                                        metadataRoot = (ObjectNode) parsed;
                                }
                        } catch (Exception ignored) {
                                metadataRoot = objectMapper.createObjectNode();
                        }
                }

                if (request.getVariants() != null) {
                        metadataRoot.set("variants", objectMapper.valueToTree(request.getVariants()));
                }

                if (request.getExtras() != null) {
                        metadataRoot.set("extras", objectMapper.valueToTree(request.getExtras()));
                }

                String metadataJson;
                try {
                        metadataJson = objectMapper.writeValueAsString(metadataRoot);
                } catch (JsonProcessingException ex) {
                        throw new IllegalArgumentException("Invalid metadata payload", ex);
                }

                int variantsCount = 0;
                int extrasCount = 0;

                JsonNode variantsNode = metadataRoot.path("variants");
                if (variantsNode.isArray()) {
                        variantsCount = variantsNode.size();
                }

                JsonNode extrasNode = metadataRoot.path("extras");
                if (extrasNode.isArray()) {
                        extrasCount = extrasNode.size();
                }

                String categorySelectSql = """
                        SELECT id
                        FROM categories
                        WHERE tenant_id = ? AND lower(name) = lower(?)
                        LIMIT 1
                        """;

                List<Long> existingCategory = jdbcTemplate.query(categorySelectSql,
                        (rs, rowNum) -> rs.getLong("id"), tenantId, category);

                Long categoryId;
                if (existingCategory.isEmpty()) {
                        if (isLocalProfile) {
                                String categoryInsertSql = """
                                INSERT INTO categories (tenant_id, name, status, display_order, created_at, updated_at)
                                VALUES (?, ?, 'ACTIVE', 0, now(), now())
                                """;
                                jdbcTemplate.update(categoryInsertSql, tenantId, category);
                                categoryId = jdbcTemplate.queryForObject(categorySelectSql, Long.class, tenantId, category);
                        } else {
                                String categoryInsertSql = """
                                INSERT INTO categories (tenant_id, name, status, display_order, created_at, updated_at)
                                VALUES (?, ?, 'ACTIVE', 0, now(), now())
                                RETURNING id
                                """;
                                categoryId = jdbcTemplate.queryForObject(categoryInsertSql, Long.class, tenantId, category);
                        }
                } else {
                        categoryId = existingCategory.get(0);
                }

                String updateSql = isLocalProfile
                        ? """
                        UPDATE tenant_products
                        SET name = ?,
                            description = ?,
                            price = ?,
                            image_url = ?,
                            department = ?,
                            vat_rate = ?,
                            available_for_order = ?,
                            metadata_json = ?,
                            updated_at = now()
                        WHERE tenant_id = ?
                          AND id = ?
                          AND COALESCE(status, 'ACTIVE') <> 'DISABLED'
                        """
                        : """
                        UPDATE tenant_products
                        SET name = ?,
                            description = ?,
                            price = ?,
                            image_url = ?,
                            department = ?,
                            vat_rate = ?,
                            available_for_order = ?,
                            metadata_json = ?::jsonb,
                            updated_at = now()
                        WHERE tenant_id = ?
                          AND id = ?
                          AND COALESCE(status, 'ACTIVE') <> 'DISABLED'
                        """;

                int updatedRows = jdbcTemplate.update(
                        updateSql,
                        name,
                        description,
                        request.getPrice(),
                        resolvedImageUrl,
                        department,
                        vatRate,
                        resolvedAvailableForOrder,
                        metadataJson,
                        tenantId,
                        productId
                );

                if (updatedRows == 0) {
                        throw new IllegalArgumentException("Product not found for tenant");
                }

                // Aggiorna il link categoria (semplice: rimuovi link esistenti e inserisci quello nuovo)
                String deleteLinksSql = """
                        DELETE FROM category_tenant_products
                        WHERE tenant_product_id = ?
                        """;
                jdbcTemplate.update(deleteLinksSql, productId);

                if (isLocalProfile) {
                        String linkSql = """
                        MERGE INTO category_tenant_products (category_id, tenant_product_id, display_order)
                        KEY (category_id, tenant_product_id)
                        VALUES (?, ?, 0)
                        """;
                        jdbcTemplate.update(linkSql, categoryId, productId);
                } else {
                        String linkSql = """
                        INSERT INTO category_tenant_products (category_id, tenant_product_id, display_order)
                        VALUES (?, ?, 0)
                        ON CONFLICT DO NOTHING
                        """;
                        jdbcTemplate.update(linkSql, categoryId, productId);
                }

                return new StaffProductDto(
                        productId,
                        name,
                        description,
                        request.getPrice(),
                        resolvedImageUrl,
                        category,
                        department,
                        vatRate,
                        status == null ? "ACTIVE" : status,
                        resolvedAvailableForOrder,
                        variantsCount,
                        extrasCount,
                        sku
                );
        }

        public boolean disableTenantProduct(Long tenantId, Long productId) {
                String sql = """
                        UPDATE tenant_products
                        SET status = 'DISABLED',
                            available_for_order = false,
                            updated_at = now()
                        WHERE tenant_id = ?
                          AND id = ?
                          AND COALESCE(status, 'ACTIVE') <> 'DISABLED'
                        """;

                return jdbcTemplate.update(sql, tenantId, productId) > 0;
        }

        public Optional<String> findTenantOrderStatus(Long tenantId, Long orderId) {
                String sql = """
                        SELECT status
                        FROM orders
                        WHERE tenant_id = ? AND id = ?
                        LIMIT 1
                        """;

                List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("status"), tenantId, orderId);
                if (rows.isEmpty()) {
                        return Optional.empty();
                }

                return Optional.ofNullable(rows.get(0));
        }

        public boolean updateTenantOrderStatus(Long tenantId, Long orderId, String newStatus, Long staffUserId) {
                String sql = """
                        UPDATE orders
                        SET status = ?,
                            delivered_by_staff_id = CASE
                                WHEN ? = 'DELIVERED' THEN COALESCE(?, delivered_by_staff_id)
                                ELSE delivered_by_staff_id
                            END,
                            updated_at = now()
                        WHERE tenant_id = ?
                          AND id = ?
                          AND status <> ?
                        """;

                int updatedRows = jdbcTemplate.update(
                        sql,
                        newStatus,
                        newStatus,
                        staffUserId,
                        tenantId,
                        orderId,
                        newStatus
                );

                return updatedRows > 0;
        }

        private String inferDepartment(String category) {
                if (category == null) {
                        return "BAR";
                }

                String normalized = category.trim().toUpperCase();
                if (normalized.equals("CIBO") || normalized.equals("SNACK") || normalized.equals("DOLCI")) {
                        return "KITCHEN";
                }

                return "BAR";
        }

        private String generateSku(String name) {
                String prefix = (name == null ? "PROD" : name)
                        .toUpperCase()
                        .replaceAll("[^A-Z0-9 ]", "")
                        .trim()
                        .replaceAll("\\s+", "");

                if (prefix.isBlank()) {
                        prefix = "PROD";
                }

                if (prefix.length() > 6) {
                        prefix = prefix.substring(0, 6);
                }

                String suffix = DateTimeFormatter.ofPattern("HHmmss").format(OffsetDateTime.now());
                int random = ThreadLocalRandom.current().nextInt(100, 1000);
                return prefix + "-" + suffix + random;
        }
}

package com.orderapp.ordering.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.dto.TenantCategoryDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TenantCategoryDto> getTenantCategories(Long tenantId) {
        String sql = """
            SELECT id, name, description, display_order, status
            FROM categories
            WHERE tenant_id = ?
            ORDER BY display_order, name
            """;

        RowMapper<TenantCategoryDto> mapper = (rs, rowNum) -> new TenantCategoryDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("display_order"),
            rs.getString("status")
        );

        return jdbcTemplate.query(sql, mapper, tenantId);
    }

    public Optional<TenantCategoryDto> getTenantCategoryById(Long tenantId, Long categoryId) {
        String sql = """
            SELECT id, name, description, display_order, status
            FROM categories
            WHERE tenant_id = ? AND id = ?
            """;

        RowMapper<TenantCategoryDto> mapper = (rs, rowNum) -> new TenantCategoryDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("display_order"),
            rs.getString("status")
        );

        try {
            TenantCategoryDto result = jdbcTemplate.queryForObject(sql, mapper, tenantId, categoryId);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public TenantCategoryDto createTenantCategory(Long tenantId, String name, String description, Integer displayOrder) {
        String sql = """
            INSERT INTO categories (tenant_id, name, description, display_order, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'ACTIVE', current_timestamp, current_timestamp)
            RETURNING id, name, description, display_order, status
            """;

        int order = displayOrder != null ? displayOrder : 0;
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, tenantId, name, description, order);

        return new TenantCategoryDto(
            ((Number) result.get("id")).longValue(),
            (String) result.get("name"),
            (String) result.get("description"),
            ((Number) result.get("display_order")).intValue(),
            (String) result.get("status")
        );
    }

    public TenantCategoryDto updateTenantCategory(Long tenantId, Long categoryId, String name, String description,
            Integer displayOrder, String status) {
        String sql = """
            UPDATE categories
            SET name = COALESCE(?, name),
                description = COALESCE(?, description),
                display_order = COALESCE(?, display_order),
                status = COALESCE(?, status),
                updated_at = current_timestamp
            WHERE tenant_id = ? AND id = ?
            RETURNING id, name, description, display_order, status
            """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, name, description, displayOrder, status, tenantId,
                categoryId);

        return new TenantCategoryDto(
            ((Number) result.get("id")).longValue(),
            (String) result.get("name"),
            (String) result.get("description"),
            ((Number) result.get("display_order")).intValue(),
            (String) result.get("status")
        );
    }

    public boolean deleteTenantCategory(Long tenantId, Long categoryId) {
        String sql = "DELETE FROM categories WHERE tenant_id = ? AND id = ?";
        int updated = jdbcTemplate.update(sql, tenantId, categoryId);
        return updated > 0;
    }

    public boolean categoryNameExists(Long tenantId, String name) {
        String sql = """
            SELECT COUNT(*) FROM categories
            WHERE tenant_id = ? AND LOWER(name) = LOWER(?)
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, name);
        return count != null && count > 0;
    }

    public boolean categoryNameExistsExcluding(Long tenantId, String name, Long excludeCategoryId) {
        String sql = """
            SELECT COUNT(*) FROM categories
            WHERE tenant_id = ? AND LOWER(name) = LOWER(?) AND id != ?
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, name, excludeCategoryId);
        return count != null && count > 0;
    }
}

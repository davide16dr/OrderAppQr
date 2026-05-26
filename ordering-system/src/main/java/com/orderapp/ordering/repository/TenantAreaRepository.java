package com.orderapp.ordering.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.dto.TenantAreaDto;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TenantAreaRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<TenantAreaDto> findActiveAreas(long tenantId) {
        return jdbcTemplate.query(
            """
            SELECT id, name, display_order, status
            FROM areas
            WHERE tenant_id = ? AND status = 'ACTIVE'
            ORDER BY display_order ASC, name ASC
            """,
            (rs, rowNum) -> new TenantAreaDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("display_order"),
                rs.getString("status")
            ),
            tenantId
        );
    }

    public Optional<TenantAreaDto> findAreaById(long tenantId, long areaId) {
        List<TenantAreaDto> rows = jdbcTemplate.query(
            """
            SELECT id, name, display_order, status
            FROM areas
            WHERE tenant_id = ? AND id = ?
            """,
            (rs, rowNum) -> new TenantAreaDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("display_order"),
                rs.getString("status")
            ),
            tenantId,
            areaId
        );

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public TenantAreaDto createArea(long tenantId, String name) {
        OffsetDateTime now = OffsetDateTime.now();
        Timestamp ts = Timestamp.from(now.toInstant());
        String normalizedName = name.trim();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                    """
                    INSERT INTO areas (tenant_id, name, display_order, status, created_at, updated_at)
                    VALUES (?, ?, 0, 'ACTIVE', ?, ?)
                    """,
                    new String[] { "id" }
                );
                ps.setLong(1, tenantId);
                ps.setString(2, normalizedName);
                ps.setTimestamp(3, ts);
                ps.setTimestamp(4, ts);
                return ps;
            }, keyHolder);
        } catch (DataIntegrityViolationException ex) {
            // unique(tenant_id, name)
            throw ex;
        }

        Number key = keyHolder.getKey();
        Long id = key != null ? key.longValue() : null;
        if (id == null) {
            id = jdbcTemplate.queryForObject(
                "SELECT id FROM areas WHERE tenant_id = ? AND name = ?",
                Long.class,
                tenantId,
                normalizedName
            );
        }

        return new TenantAreaDto(id, normalizedName, 0, "ACTIVE");
    }

    public boolean disableArea(long tenantId, long areaId) {
        Timestamp ts = Timestamp.from(OffsetDateTime.now().toInstant());

        // Detach locations from this area for the tenant.
        jdbcTemplate.update(
            """
            UPDATE locations
            SET area_id = NULL,
                updated_at = ?
            WHERE tenant_id = ? AND area_id = ?
            """,
            ts,
            tenantId,
            areaId
        );

        int updated = jdbcTemplate.update(
            """
            UPDATE areas
            SET status = 'DISABLED',
                updated_at = ?
            WHERE tenant_id = ? AND id = ? AND status = 'ACTIVE'
            """,
            ts,
            tenantId,
            areaId
        );

        return updated > 0;
    }

    public TenantAreaDto updateArea(long tenantId, long areaId, String name, Integer displayOrder, String status) {
        Timestamp ts = Timestamp.from(OffsetDateTime.now().toInstant());

        int updated = jdbcTemplate.update(
            """
            UPDATE areas
            SET name = COALESCE(?, name),
                display_order = COALESCE(?, display_order),
                status = COALESCE(?, status),
                updated_at = ?
            WHERE tenant_id = ? AND id = ?
            """,
            normalizeName(name),
            displayOrder,
            normalizeStatus(status),
            ts,
            tenantId,
            areaId
        );

        if (updated == 0) {
            return null;
        }

        return findAreaById(tenantId, areaId).orElse(null);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"DISABLED".equals(normalized)) {
            return null;
        }
        return normalized;
    }
}

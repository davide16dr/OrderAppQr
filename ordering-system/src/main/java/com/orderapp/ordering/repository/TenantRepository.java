package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.Tenant;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findBySlugIgnoreCase(String slug);
    Optional<Tenant> findBySubdomainIgnoreCase(String subdomain);
    Optional<Tenant> findByBusinessEmailIgnoreCase(String email);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE tenants SET opening_config_json = CAST(:json AS jsonb), updated_at = :updatedAt WHERE id = :tenantId", nativeQuery = true)
    int updateOpeningConfigJson(@Param("tenantId") Long tenantId, @Param("json") String json, @Param("updatedAt") OffsetDateTime updatedAt);
}

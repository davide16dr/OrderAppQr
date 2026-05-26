package com.orderapp.ordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.AreaEntity;

@Repository
public interface AreaRepository extends JpaRepository<AreaEntity, Long> {
    List<AreaEntity> findByTenantIdOrderByDisplayOrderAscNameAsc(Long tenantId);

    List<AreaEntity> findByTenantIdAndStatusOrderByDisplayOrderAscNameAsc(Long tenantId, String status);

    Optional<AreaEntity> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(Long tenantId, String name, Long id);
}
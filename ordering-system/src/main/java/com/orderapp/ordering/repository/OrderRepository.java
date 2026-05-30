package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.OrderEntity;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<OrderEntity> findByTenantIdAndLocationIdOrderByCreatedAtDesc(Long tenantId, Long locationId);
    java.util.Optional<OrderEntity> findByIdAndTenantId(Long id, Long tenantId);
}

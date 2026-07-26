package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.OrderEntity;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<OrderEntity> findByTenantIdAndLocationIdOrderByCreatedAtDesc(Long tenantId, Long locationId);
    java.util.Optional<OrderEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT o.tenantId, COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :from GROUP BY o.tenantId")
    List<Object[]> countOrdersGroupedByTenantSince(@Param("from") OffsetDateTime from);
}

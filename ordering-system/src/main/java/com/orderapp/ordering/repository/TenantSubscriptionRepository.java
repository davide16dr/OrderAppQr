package com.orderapp.ordering.repository;

import com.orderapp.ordering.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {
    
    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.tenant.id = :tenantId AND ts.status IN ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE')")
    Optional<TenantSubscription> findCurrentSubscriptionByTenantId(@Param("tenantId") Long tenantId);
    
    List<TenantSubscription> findByTenantId(Long tenantId);
    
    List<TenantSubscription> findByStatus(String status);
}

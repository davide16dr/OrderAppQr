package com.orderapp.ordering.repository;

import com.orderapp.ordering.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.tenant.id = :tenantId AND ts.status IN ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE', 'EXPIRED', 'CANCELLED') ORDER BY ts.createdAt DESC")
    Optional<TenantSubscription> findCurrentSubscriptionByTenantId(@Param("tenantId") Long tenantId);

    /** Expired TRIAL subscriptions whose trialEndsAt has passed the cutoff (for daily scheduler). */
    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.status = 'TRIAL' AND ts.trialEndsAt < :cutoff")
    List<TenantSubscription> findExpiredTrialsBefore(@Param("cutoff") OffsetDateTime cutoff);

    List<TenantSubscription> findByTenantId(Long tenantId);

    List<TenantSubscription> findByStatus(String status);

    Optional<TenantSubscription> findByProviderSubscriptionId(String providerSubscriptionId);

    /** Subscriptions with currentPeriodEnd falling in [start, end] — for reminder emails. */
    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.status = 'ACTIVE' AND ts.currentPeriodEnd BETWEEN :start AND :end")
    List<TenantSubscription> findActiveExpiringBetween(@Param("start") OffsetDateTime start,
                                                       @Param("end") OffsetDateTime end);

    /** Active subscriptions whose period ended before :cutoff — for auto-deactivation. */
    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.status = 'ACTIVE' AND ts.currentPeriodEnd < :cutoff")
    List<TenantSubscription> findActiveExpiredBefore(@Param("cutoff") OffsetDateTime cutoff);
}

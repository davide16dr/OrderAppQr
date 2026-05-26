package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.entity.SubscriptionPlan;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantSubscriptionService {
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public Optional<TenantSubscription> getCurrentSubscription(Long tenantId) {
        return tenantSubscriptionRepository.findCurrentSubscriptionByTenantId(tenantId);
    }

    public List<TenantSubscription> getSubscriptionsByTenantId(Long tenantId) {
        return tenantSubscriptionRepository.findByTenantId(tenantId);
    }

    public TenantSubscription createSubscription(Long tenantId, String planCode, String billingCycle) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planCode)
            .orElseThrow(() -> new RuntimeException("Plan not found: " + planCode));

        TenantSubscription subscription = TenantSubscription.builder()
            .tenant(tenant)
            .subscriptionPlan(plan)
            .status("PENDING")
            .billingCycle(billingCycle)
            .paymentStatus("PENDING")
            .build();

        return tenantSubscriptionRepository.save(subscription);
    }

    public TenantSubscription activateSubscription(Long subscriptionId) {
        TenantSubscription subscription = tenantSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        subscription.setStatus("ACTIVE");
        subscription.setPaymentStatus("PAID");
        subscription.setActivatedAt(OffsetDateTime.now());
        subscription.setCurrentPeriodStart(OffsetDateTime.now());

        // Calcola la fine del periodo in base al ciclo di fatturazione
        if ("MONTHLY".equals(subscription.getBillingCycle())) {
            subscription.setCurrentPeriodEnd(OffsetDateTime.now().plusMonths(1));
        } else if ("YEARLY".equals(subscription.getBillingCycle())) {
            subscription.setCurrentPeriodEnd(OffsetDateTime.now().plusYears(1));
        }

        return tenantSubscriptionRepository.save(subscription);
    }

    public TenantSubscription cancelSubscription(Long subscriptionId) {
        TenantSubscription subscription = tenantSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        subscription.setStatus("CANCELLED");
        subscription.setCancelledAt(OffsetDateTime.now());

        return tenantSubscriptionRepository.save(subscription);
    }

    public List<TenantSubscription> getSubscriptionsByStatus(String status) {
        return tenantSubscriptionRepository.findByStatus(status);
    }

    public TenantSubscription updateSubscription(Long subscriptionId, TenantSubscription updatedSubscription) {
        TenantSubscription subscription = tenantSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        if (updatedSubscription.getStatus() != null) {
            subscription.setStatus(updatedSubscription.getStatus());
        }
        if (updatedSubscription.getPaymentStatus() != null) {
            subscription.setPaymentStatus(updatedSubscription.getPaymentStatus());
        }
        if (updatedSubscription.getProviderCustomerId() != null) {
            subscription.setProviderCustomerId(updatedSubscription.getProviderCustomerId());
        }
        if (updatedSubscription.getProviderSubscriptionId() != null) {
            subscription.setProviderSubscriptionId(updatedSubscription.getProviderSubscriptionId());
        }

        return tenantSubscriptionRepository.save(subscription);
    }
}

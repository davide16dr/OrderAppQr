package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalScheduler {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    /**
     * Runs daily at 08:00 Europe/Rome.
     * Sends a renewal reminder to tenants whose subscription expires in exactly 5 days.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Rome")
    @Transactional(readOnly = true)
    public void sendRenewalReminders() {
        OffsetDateTime windowStart = OffsetDateTime.now().plusDays(4).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime windowEnd   = OffsetDateTime.now().plusDays(6).withHour(23).withMinute(59).withSecond(59).withNano(0);

        List<TenantSubscription> expiring = subscriptionRepository.findActiveExpiringBetween(windowStart, windowEnd);
        log.info("Renewal reminder check: {} subscription(s) expiring around 5 days from now", expiring.size());

        for (TenantSubscription sub : expiring) {
            Tenant tenant = sub.getTenant();
            String email  = tenant.getBusinessEmail();
            if (email == null || email.isBlank()) continue;

            String planName = sub.getSubscriptionPlan() != null ? sub.getSubscriptionPlan().getName() : "Piano attivo";

            boolean sent = emailService.sendRenewalReminderEmail(
                    email,
                    tenant.getName(),
                    planName,
                    sub.getCurrentPeriodEnd()
            );

            if (sent) {
                log.info("Renewal reminder sent to tenant {} ({})", tenant.getId(), email);
            } else {
                log.warn("Could not send renewal reminder to tenant {} ({})", tenant.getId(), email);
            }
        }
    }

    /**
     * Runs daily at 09:00 Europe/Rome.
     * Marks TRIAL subscriptions as EXPIRED when trialEndsAt has passed.
     * The tenant stays ACTIVE so the user can still log in and subscribe.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Rome")
    @Transactional
    public void deactivateExpiredTrials() {
        OffsetDateTime now = OffsetDateTime.now();
        List<TenantSubscription> expiredTrials = subscriptionRepository.findExpiredTrialsBefore(now);
        log.info("Trial expiry check: {} trial(s) expired", expiredTrials.size());

        for (TenantSubscription sub : expiredTrials) {
            sub.setStatus("EXPIRED");
            sub.setPaymentStatus("NONE");
            subscriptionRepository.save(sub);

            Tenant tenant = sub.getTenant();
            String email = tenant.getBusinessEmail();
            if (email != null && !email.isBlank()) {
                emailService.sendSubscriptionExpiredEmail(email, tenant.getName());
            }
            log.info("Trial expired for tenant {} — account remains accessible for renewal", tenant.getId());
        }
    }

    /**
     * Runs daily at 09:30 Europe/Rome.
     * Marks ACTIVE subscriptions as EXPIRED if they ended more than 5 days ago (Stripe webhook missed).
     * The tenant stays ACTIVE so the user can still log in and renew.
     */
    @Scheduled(cron = "0 30 9 * * *", zone = "Europe/Rome")
    @Transactional
    public void deactivateExpiredSubscriptions() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(5);

        List<TenantSubscription> expired = subscriptionRepository.findActiveExpiredBefore(cutoff);
        log.info("Subscription expiry check: {} subscription(s) expired before {}", expired.size(), cutoff);

        for (TenantSubscription sub : expired) {
            Tenant tenant = sub.getTenant();

            sub.setStatus("EXPIRED");
            sub.setPaymentStatus("OVERDUE");
            subscriptionRepository.save(sub);

            String email = tenant.getBusinessEmail();
            if (email != null && !email.isBlank()) {
                emailService.sendSubscriptionExpiredEmail(email, tenant.getName());
            }

            log.warn("Subscription {} expired for tenant {} — account remains accessible for renewal",
                    sub.getId(), tenant.getId());
        }
    }
}

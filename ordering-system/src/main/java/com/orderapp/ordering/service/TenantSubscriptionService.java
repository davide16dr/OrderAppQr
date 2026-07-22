package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.entity.SubscriptionPlan;
import com.orderapp.ordering.model.dto.TenantSubscriptionResponseDto;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.SubscriptionPlanRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSubscriptionService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final StripeService stripeService;

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ROME);

    // ── Read ──────────────────────────────────────────────────────────────────

    public Optional<TenantSubscription> getCurrentSubscription(Long tenantId) {
        return tenantSubscriptionRepository.findCurrentSubscriptionByTenantId(tenantId);
    }

    public Optional<TenantSubscriptionResponseDto> getCurrentSubscriptionDto(Long tenantId) {
        return tenantSubscriptionRepository.findCurrentSubscriptionByTenantId(tenantId)
                .map(this::toDto);
    }

    public List<TenantSubscription> getSubscriptionsByTenantId(Long tenantId) {
        return tenantSubscriptionRepository.findByTenantId(tenantId);
    }

    // ── Stripe actions ────────────────────────────────────────────────────────

    @Transactional
    public TenantSubscriptionResponseDto cancelAtPeriodEnd(Long tenantId) {
        TenantSubscription sub = requireCurrentSub(tenantId);
        requireStripe(sub);
        try {
            stripeService.cancelAtPeriodEnd(sub.getProviderSubscriptionId());
            sub.setCancelAtPeriodEnd(true);
            tenantSubscriptionRepository.save(sub);
            log.info("Tenant {} subscription set to cancel at period end", tenantId);
            return toDto(sub);
        } catch (StripeException e) {
            throw new IllegalStateException("Impossibile disdire l'abbonamento: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TenantSubscriptionResponseDto reactivate(Long tenantId) {
        TenantSubscription sub = requireCurrentSub(tenantId);
        requireStripe(sub);
        if (!sub.isCancelAtPeriodEnd()) {
            throw new IllegalStateException("L'abbonamento non è in stato di disdetta");
        }
        try {
            stripeService.reactivateSubscription(sub.getProviderSubscriptionId());
            sub.setCancelAtPeriodEnd(false);
            tenantSubscriptionRepository.save(sub);
            log.info("Tenant {} subscription reactivated", tenantId);
            return toDto(sub);
        } catch (StripeException e) {
            throw new IllegalStateException("Impossibile riattivare l'abbonamento: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TenantSubscriptionResponseDto changeBillingCycle(Long tenantId, String newCycle) {
        TenantSubscription sub = requireCurrentSub(tenantId);
        requireStripe(sub);
        if (sub.getBillingCycle().equalsIgnoreCase(newCycle)) {
            throw new IllegalStateException("L'abbonamento è già in ciclo " + newCycle);
        }
        SubscriptionPlan plan = sub.getSubscriptionPlan();
        String newPriceId = "YEARLY".equalsIgnoreCase(newCycle)
                ? plan.getStripePriceIdYearly()
                : plan.getStripePriceIdMonthly();
        if (newPriceId == null || newPriceId.isBlank()) {
            throw new IllegalStateException("Nessun prezzo Stripe configurato per il ciclo " + newCycle);
        }
        try {
            stripeService.changeBillingCycle(sub.getProviderSubscriptionId(), newPriceId);
            sub.setBillingCycle(newCycle.toUpperCase());
            tenantSubscriptionRepository.save(sub);
            log.info("Tenant {} billing cycle changed to {}", tenantId, newCycle);
            return toDto(sub);
        } catch (StripeException e) {
            throw new IllegalStateException("Impossibile cambiare il ciclo: " + e.getMessage(), e);
        }
    }

    @Transactional
    public String createRenewalCheckoutSession(Long tenantId, String billingCycle, String customerEmail) {
        TenantSubscription sub = tenantSubscriptionRepository.findCurrentSubscriptionByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Nessun abbonamento trovato per il tenant " + tenantId));
        SubscriptionPlan plan = sub.getSubscriptionPlan();
        if (plan == null) throw new IllegalStateException("Piano non trovato per l'abbonamento");
        String cycle = (billingCycle != null && "YEARLY".equalsIgnoreCase(billingCycle)) ? "YEARLY" : "MONTHLY";
        String priceId = "YEARLY".equals(cycle) ? plan.getStripePriceIdYearly() : plan.getStripePriceIdMonthly();
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException("Nessun prezzo Stripe configurato per il ciclo " + cycle);
        }
        try {
            // Se l'utente è in trial attivo, Stripe deve sapere che il trial finisce alla data già stabilita
            // (non 14 giorni freschi). Se scaduto/cancellato, nessun trial.
            Long trialEndEpoch = null;
            if ("TRIAL".equalsIgnoreCase(sub.getStatus()) && sub.getTrialEndsAt() != null
                    && sub.getTrialEndsAt().isAfter(OffsetDateTime.now())) {
                trialEndEpoch = sub.getTrialEndsAt().toEpochSecond();
            }
            String url = stripeService.createCheckoutSessionWithTrialEnd(
                    tenantId, sub.getId(), priceId, customerEmail, trialEndEpoch);
            sub.setBillingCycle(cycle);
            tenantSubscriptionRepository.save(sub);
            log.info("Checkout session created for tenant {} ({}) trialEnd={}", tenantId, cycle, trialEndEpoch);
            return url;
        } catch (StripeException e) {
            throw new IllegalStateException("Errore Stripe: " + e.getMessage(), e);
        }
    }

    public String createPortalSession(Long tenantId, String returnUrl) {
        TenantSubscription sub = requireCurrentSub(tenantId);
        if (sub.getProviderCustomerId() == null) {
            throw new IllegalStateException("Nessun customer Stripe associato a questo tenant");
        }
        try {
            return stripeService.createBillingPortalSession(sub.getProviderCustomerId(), returnUrl);
        } catch (StripeException e) {
            throw new IllegalStateException("Impossibile aprire il portale: " + e.getMessage(), e);
        }
    }

    // ── Legacy CRUD (keep for internal use) ───────────────────────────────────

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
        TenantSubscription sub = tenantSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        sub.setStatus("ACTIVE");
        sub.setPaymentStatus("PAID");
        sub.setActivatedAt(OffsetDateTime.now());
        sub.setCurrentPeriodStart(OffsetDateTime.now());
        sub.setCurrentPeriodEnd("MONTHLY".equals(sub.getBillingCycle())
                ? OffsetDateTime.now().plusMonths(1)
                : OffsetDateTime.now().plusYears(1));
        return tenantSubscriptionRepository.save(sub);
    }

    public List<TenantSubscription> getSubscriptionsByStatus(String status) {
        return tenantSubscriptionRepository.findByStatus(status);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TenantSubscription requireCurrentSub(Long tenantId) {
        return tenantSubscriptionRepository.findCurrentSubscriptionByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Nessun abbonamento attivo per il tenant " + tenantId));
    }

    private void requireStripe(TenantSubscription sub) {
        if (sub.getProviderSubscriptionId() == null) {
            throw new IllegalStateException("Nessuna sottoscrizione Stripe associata");
        }
    }

    private TenantSubscriptionResponseDto toDto(TenantSubscription sub) {
        SubscriptionPlan plan = sub.getSubscriptionPlan();
        return TenantSubscriptionResponseDto.builder()
                .id(sub.getId())
                .planCode(plan != null ? plan.getCode() : null)
                .planName(plan != null ? plan.getName() : null)
                .priceMonthly(plan != null ? plan.getPriceMonthly() : null)
                .priceYearly(plan != null ? plan.getPriceYearly() : null)
                .status(sub.getStatus())
                .paymentStatus(sub.getPaymentStatus())
                .billingCycle(sub.getBillingCycle())
                .currentPeriodEnd(sub.getCurrentPeriodEnd() != null
                        ? sub.getCurrentPeriodEnd().format(DATE_FMT) : null)
                .trialEndsAt(sub.getTrialEndsAt() != null
                        ? sub.getTrialEndsAt().format(DATE_FMT) : null)
                .activatedAt(sub.getActivatedAt() != null
                        ? sub.getActivatedAt().format(DATE_FMT) : null)
                .cancelAtPeriodEnd(sub.isCancelAtPeriodEnd())
                .hasStripeSubscription(sub.getProviderSubscriptionId() != null)
                .build();
    }
}

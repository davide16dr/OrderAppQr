package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${app.stripe.secret-key}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.stripe.frontend-url}")
    private String frontendUrl;

    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe initialized (key prefix: {})", secretKey.length() > 8 ? secretKey.substring(0, 8) + "..." : "<short>");
    }

    private static final int TRIAL_DAYS = 14;

    /**
     * Creates a Stripe Checkout Session and returns the hosted payment URL.
     * Uses SUBSCRIPTION mode with a 14-day free trial.
     */
    public String createCheckoutSession(Long tenantId, Long subscriptionId,
                                        String stripePriceId, String customerEmail) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(frontendUrl + "/public/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/public/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(stripePriceId)
                        .setQuantity(1L)
                        .build())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .setTrialPeriodDays((long) TRIAL_DAYS)
                        .build())
                .putMetadata("subscriptionId", subscriptionId.toString())
                .putMetadata("tenantId", tenantId.toString())
                .build();

        Session session = Session.create(params);
        log.info("Stripe Checkout Session created: {} for tenant {} sub {} (trial: {} days)",
                 session.getId(), tenantId, subscriptionId, TRIAL_DAYS);
        return session.getUrl();
    }

    /**
     * Verifies the Stripe webhook signature and dispatches the event.
     * Must receive the raw (unparsed) request body.
     */
    @Transactional
    public void handleWebhook(String rawPayload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(rawPayload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature invalid: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        log.info("Stripe webhook received: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed"    -> handleCheckoutCompleted(event);
            case "invoice.paid"                  -> handleInvoicePaid(event);
            case "invoice.payment_failed"        -> handlePaymentFailed(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.debug("Stripe event ignored: {}", event.getType());
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private StripeObject deserialize(Event event) {
        EventDataObjectDeserializer d = event.getDataObjectDeserializer();
        if (d.getObject().isPresent()) return d.getObject().get();
        try {
            return d.deserializeUnsafe();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize Stripe event " + event.getType() + ": " + ex.getMessage(), ex);
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) deserialize(event);

        String subscriptionIdStr = session.getMetadata().get("subscriptionId");
        if (subscriptionIdStr == null) {
            log.warn("checkout.session.completed missing subscriptionId metadata");
            return;
        }

        Long subscriptionId = Long.parseLong(subscriptionIdStr);
        TenantSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalStateException("Subscription not found: " + subscriptionId));

        // Idempotency: already processed if providerSubscriptionId is set
        if (sub.getProviderSubscriptionId() != null) {
            log.info("checkout.session.completed already processed for subscription {}, skipping", subscriptionId);
            return;
        }

        Tenant tenant = sub.getTenant();

        // Activate tenant
        tenant.setStatus("ACTIVE");
        tenant.setEnabled(true);
        tenant.setActivationDate(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());
        tenantRepository.save(tenant);

        // Activate subscription — PENDING until first invoice.paid confirms payment
        sub.setStatus("ACTIVE");
        sub.setPaymentStatus("PENDING");
        sub.setPaymentProvider("STRIPE");
        sub.setProviderCustomerId(session.getCustomer());
        sub.setProviderSubscriptionId(session.getSubscription());
        sub.setActivatedAt(OffsetDateTime.now());
        sub.setTrialEndsAt(OffsetDateTime.now().plusDays(TRIAL_DAYS));
        sub.setCurrentPeriodStart(OffsetDateTime.now());
        sub.setCurrentPeriodEnd(OffsetDateTime.now().plusDays(TRIAL_DAYS));
        subscriptionRepository.save(sub);

        log.info("Tenant {} activated via Stripe checkout session {}", tenant.getId(), session.getId());
    }

    private void handleInvoicePaid(Event event) {
        Invoice invoice = (Invoice) deserialize(event);

        String stripeSubId = invoice.getSubscription();
        if (stripeSubId == null) return;

        // Skip €0 invoices generated at trial start — only process real payments
        if (invoice.getAmountPaid() != null && invoice.getAmountPaid() == 0L) {
            log.info("Skipping zero-amount invoice {} (trial start)", invoice.getId());
            return;
        }

        subscriptionRepository.findByProviderSubscriptionId(stripeSubId).ifPresent(sub -> {
            sub.setPaymentStatus("PAID");
            sub.setStatus("ACTIVE");

            // Update period from Stripe subscription object
            if (invoice.getLines() != null) {
                long periodEnd = invoice.getLines().getData().stream()
                        .findFirst()
                        .map(line -> line.getPeriod().getEnd())
                        .orElse(0L);
                if (periodEnd > 0) {
                    sub.setCurrentPeriodEnd(OffsetDateTime.ofInstant(Instant.ofEpochSecond(periodEnd), ZoneOffset.UTC));
                }
            }

            subscriptionRepository.save(sub);
            log.info("Subscription {} renewed via invoice {}", sub.getId(), invoice.getId());
        });
    }

    private void handlePaymentFailed(Event event) {
        Invoice invoice = (Invoice) deserialize(event);

        String stripeSubId = invoice.getSubscription();
        if (stripeSubId == null) return;

        subscriptionRepository.findByProviderSubscriptionId(stripeSubId).ifPresent(sub -> {
            sub.setPaymentStatus("FAILED");
            sub.setStatus("PAST_DUE");
            subscriptionRepository.save(sub);

            Tenant tenant = sub.getTenant();
            String email = tenant.getBusinessEmail();
            if (email != null) {
                emailService.sendPaymentFailedEmail(email, tenant.getName());
            }

            log.warn("Payment failed for subscription {} (tenant {})", sub.getId(), tenant.getId());
        });
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription stripeSub = (Subscription) deserialize(event);

        subscriptionRepository.findByProviderSubscriptionId(stripeSub.getId()).ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            sub.setCancelledAt(OffsetDateTime.now());
            subscriptionRepository.save(sub);

            Tenant tenant = sub.getTenant();
            tenant.setStatus("SUSPENDED");
            tenant.setEnabled(false);
            tenant.setUpdatedAt(OffsetDateTime.now());
            tenantRepository.save(tenant);

            log.info("Tenant {} suspended after Stripe subscription {} deleted", tenant.getId(), stripeSub.getId());
        });
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription stripeSub = (Subscription) deserialize(event);

        subscriptionRepository.findByProviderSubscriptionId(stripeSub.getId()).ifPresent(sub -> {
            sub.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSub.getCancelAtPeriodEnd()));

            if (stripeSub.getCurrentPeriodEnd() != null) {
                sub.setCurrentPeriodEnd(OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()), ZoneOffset.UTC));
            }
            if (stripeSub.getCurrentPeriodStart() != null) {
                sub.setCurrentPeriodStart(OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()), ZoneOffset.UTC));
            }

            // Sync billing cycle from Stripe interval
            if (stripeSub.getItems() != null && !stripeSub.getItems().getData().isEmpty()) {
                String interval = stripeSub.getItems().getData().get(0).getPlan().getInterval();
                sub.setBillingCycle("year".equals(interval) ? "YEARLY" : "MONTHLY");
            }

            subscriptionRepository.save(sub);
            log.info("Subscription {} updated (cancelAtPeriodEnd={}, status={})",
                    sub.getId(), sub.isCancelAtPeriodEnd(), stripeSub.getStatus());
        });
    }

    // ── Public Stripe actions ─────────────────────────────────────────────────

    public void cancelAtPeriodEnd(String providerSubscriptionId) throws StripeException {
        Subscription stripeSub = Subscription.retrieve(providerSubscriptionId);
        stripeSub.update(SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build());
        log.info("Stripe subscription {} set to cancel at period end", providerSubscriptionId);
    }

    public void reactivateSubscription(String providerSubscriptionId) throws StripeException {
        Subscription stripeSub = Subscription.retrieve(providerSubscriptionId);
        stripeSub.update(SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(false)
                .build());
        log.info("Stripe subscription {} reactivated (cancel_at_period_end=false)", providerSubscriptionId);
    }

    public void changeBillingCycle(String providerSubscriptionId, String newPriceId) throws StripeException {
        Subscription stripeSub = Subscription.retrieve(providerSubscriptionId);
        String itemId = stripeSub.getItems().getData().get(0).getId();
        stripeSub.update(SubscriptionUpdateParams.builder()
                .addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(itemId)
                        .setPrice(newPriceId)
                        .build())
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build());
        log.info("Stripe subscription {} price changed to {}", providerSubscriptionId, newPriceId);
    }

    public String createBillingPortalSession(String customerId, String returnUrl) throws StripeException {
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(customerId)
                        .setReturnUrl(returnUrl)
                        .build();
        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);
        log.info("Billing portal session created for customer {}", customerId);
        return session.getUrl();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OffsetDateTime computePeriodEnd(String billingCycle) {
        return "YEARLY".equalsIgnoreCase(billingCycle)
                ? OffsetDateTime.now().plusYears(1)
                : OffsetDateTime.now().plusMonths(1);
    }
}

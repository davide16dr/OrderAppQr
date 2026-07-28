package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.StripeObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("StripeService — webhook handler tests")
class StripeServiceTest {

    @InjectMocks
    private StripeService stripeService;

    @Mock
    private TenantSubscriptionRepository subscriptionRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_dummy");
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_dummy");
        ReflectionTestUtils.setField(stripeService, "frontendUrl", "http://localhost:4200");
        stripeService.init();
    }

    // ── handleInvoicePaid ──────────────────────────────────────────────────

    @Test
    @DisplayName("invoice.paid: zero-amount invoice is skipped (trial start)")
    void invoicePaid_skipsZeroAmountInvoice() {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getSubscription()).thenReturn("sub_123");
        when(invoice.getAmountPaid()).thenReturn(0L);
        when(invoice.getId()).thenReturn("inv_zero");

        stripeService.handleInvoicePaid(mockEventWith(invoice));

        verify(subscriptionRepository, never()).findByProviderSubscriptionId(any());
    }

    @Test
    @DisplayName("invoice.paid: uses MAX period end across invoice lines (not first)")
    void invoicePaid_usesMaxPeriodEnd() {
        long trialEnd   = 1_722_895_200L; // earlier — trial line
        long billingEnd = 1_754_431_200L; // later  — real billing line

        // Build lines before stubbing to avoid nested when() calls
        InvoiceLineItem line1 = buildInvoiceLine(trialEnd);
        InvoiceLineItem line2 = buildInvoiceLine(billingEnd);

        Invoice invoice = mock(Invoice.class, Answers.RETURNS_DEEP_STUBS);
        when(invoice.getSubscription()).thenReturn("sub_123");
        when(invoice.getAmountPaid()).thenReturn(5900L);
        when(invoice.getId()).thenReturn("inv_paid");
        when(invoice.getLines().getData()).thenReturn(List.of(line1, line2));

        TenantSubscription sub = new TenantSubscription();
        Tenant tenant = new Tenant();
        sub.setTenant(tenant);

        when(subscriptionRepository.findByProviderSubscriptionId("sub_123"))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenReturn(sub);

        stripeService.handleInvoicePaid(mockEventWith(invoice));

        assertNotNull(sub.getCurrentPeriodEnd());
        assertEquals(billingEnd, sub.getCurrentPeriodEnd().toEpochSecond());
    }

    @Test
    @DisplayName("invoice.paid: sends renewal email to tenant owner")
    void invoicePaid_sendsRenewalEmail() {
        long periodEnd = 1_754_431_200L;

        InvoiceLineItem line = buildInvoiceLine(periodEnd);

        Invoice invoice = mock(Invoice.class, Answers.RETURNS_DEEP_STUBS);
        when(invoice.getSubscription()).thenReturn("sub_123");
        when(invoice.getAmountPaid()).thenReturn(5900L);
        when(invoice.getId()).thenReturn("inv_paid");
        when(invoice.getLines().getData()).thenReturn(List.of(line));

        Tenant tenant = new Tenant();
        tenant.setName("Ristorante Test");
        tenant.setBusinessEmail("owner@test.com");

        TenantSubscription sub = new TenantSubscription();
        sub.setTenant(tenant);

        when(subscriptionRepository.findByProviderSubscriptionId("sub_123"))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenReturn(sub);

        stripeService.handleInvoicePaid(mockEventWith(invoice));

        verify(emailService, times(1))
                .sendRenewalSuccessEmail(eq("owner@test.com"), eq("Ristorante Test"), any(), any());
    }

    // ── handlePaymentFailed ────────────────────────────────────────────────

    @Test
    @DisplayName("invoice.payment_failed: sets PAST_DUE and sends email")
    void paymentFailed_setsPastDueAndSendsEmail() {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getSubscription()).thenReturn("sub_456");

        Tenant tenant = new Tenant();
        tenant.setName("Pizzeria Test");
        tenant.setBusinessEmail("pizza@test.com");

        TenantSubscription sub = new TenantSubscription();
        sub.setTenant(tenant);

        when(subscriptionRepository.findByProviderSubscriptionId("sub_456"))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenReturn(sub);

        stripeService.handlePaymentFailed(mockEventWith(invoice));

        assertEquals("PAST_DUE", sub.getStatus());
        assertEquals("FAILED", sub.getPaymentStatus());
        verify(emailService, times(1)).sendPaymentFailedEmail("pizza@test.com", "Pizzeria Test");
    }

    // ── handleSubscriptionDeleted ──────────────────────────────────────────

    @Test
    @DisplayName("customer.subscription.deleted: cancels subscription and suspends tenant")
    void subscriptionDeleted_cancelsAndSuspendsTenant() {
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getId()).thenReturn("sub_789");

        Tenant tenant = new Tenant();
        tenant.setStatus("ACTIVE");
        tenant.setEnabled(true);

        TenantSubscription sub = new TenantSubscription();
        sub.setTenant(tenant);

        when(subscriptionRepository.findByProviderSubscriptionId("sub_789"))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenReturn(sub);
        when(tenantRepository.save(any())).thenReturn(tenant);

        stripeService.handleSubscriptionDeleted(mockEventWith(stripeSub));

        assertEquals("CANCELLED", sub.getStatus());
        assertNotNull(sub.getCancelledAt());
        assertEquals("SUSPENDED", tenant.getStatus());
        assertFalse(tenant.isEnabled());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Event mockEventWith(StripeObject stripeObject) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeObject));
        return event;
    }

    private InvoiceLineItem buildInvoiceLine(long endEpoch) {
        InvoiceLineItem line = mock(InvoiceLineItem.class, Answers.RETURNS_DEEP_STUBS);
        when(line.getPeriod().getEnd()).thenReturn(endEpoch);
        return line;
    }
}

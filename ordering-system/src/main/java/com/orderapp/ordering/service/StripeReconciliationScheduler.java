package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.stripe.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reconcilia automaticamente lo stato degli abbonamenti Stripe nel DB.
 * Corregge i casi in cui un webhook non è arrivato o è arrivato con dati errati:
 *   - tenant ACTIVE con paymentStatus != PAID (webhook invoice.paid mancato)
 *   - tenant ACTIVE con periodo scaduto da >1 giorno (webhook di rinnovo mancato)
 *
 * Gira 2 minuti dopo l'avvio (fix immediato per dati esistenti) e poi ogni 24h alle 4:00.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeReconciliationScheduler {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;

    @Scheduled(initialDelay = 120_000, fixedDelay = Long.MAX_VALUE)
    public void reconcileOnStartup() {
        reconcile("startup");
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Rome")
    public void reconcileDaily() {
        reconcile("daily");
    }

    private void reconcile(String trigger) {
        OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);
        List<TenantSubscription> suspects = subscriptionRepository.findSubscriptionsNeedingReconciliation(yesterday);

        if (suspects.isEmpty()) {
            log.info("Stripe reconciliation [{}]: nessuna subscription da correggere", trigger);
            return;
        }

        log.info("Stripe reconciliation [{}]: trovate {} subscription da sincronizzare", trigger, suspects.size());
        int fixed = 0;
        int failed = 0;

        for (TenantSubscription sub : suspects) {
            Long tenantId = sub.getTenant().getId();
            try {
                stripeService.syncFromStripe(tenantId);
                log.info("Stripe reconciliation: tenant {} sincronizzato OK", tenantId);
                fixed++;
            } catch (InvalidRequestException e) {
                if ("resource_missing".equals(e.getCode())) {
                    // ID Stripe non esiste in questo ambiente (es. ID sandbox dopo passaggio a live)
                    // Puliamo gli ID così non vengono ricontrollati ogni notte
                    log.warn("Stripe reconciliation: tenant {} — subscription ID non trovata in Stripe ({}), pulisco IDs sandbox",
                            tenantId, sub.getProviderSubscriptionId());
                    sub.setProviderSubscriptionId(null);
                    sub.setProviderCustomerId(null);
                    subscriptionRepository.save(sub);
                } else {
                    log.warn("Stripe reconciliation: tenant {} fallito — {}", tenantId, e.getMessage());
                }
                failed++;
            } catch (Exception e) {
                log.warn("Stripe reconciliation: tenant {} fallito — {}", tenantId, e.getMessage());
                failed++;
            }
        }

        log.info("Stripe reconciliation [{}] completata: {} OK, {} falliti", trigger, fixed, failed);
    }
}

package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.StaffUser;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.repository.StaffUserRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.orderapp.ordering.model.dto.TenantDetailDto;
import com.orderapp.ordering.model.dto.TenantSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final StaffUserRepository staffUserRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Recupera tutti i tenant con paginazione.
     * Risultato cachato per migliorare performance.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "allTenants", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<TenantSummaryDto> getAllTenants(
            Pageable pageable) {
        log.debug("Fetching all tenants with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Tenant> tenants = tenantRepository.findAll(pageable);
        log.info("Retrieved {} tenants from {} total", tenants.getNumberOfElements(), tenants.getTotalElements());
        
        return tenants.map(this::toDto);
    }
    
    /**
     * Recupera i primi N tenant senza paginazione (per legacy o use case specifici).
     */
    @Transactional(readOnly = true)
    public List<TenantSummaryDto> getFirstTenants(int limit) {
        log.debug("Fetching first {} tenants", limit);
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        return tenantRepository.findAll(pageable).stream()
                .map(this::toDto)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Aggiorna lo stato abilitazione del tenant.
     * @param tenantId ID del tenant
     * @param isEnabled stato desiderato
     * @throws EntityNotFoundException se il tenant non esiste
     */
    @Transactional
    @CacheEvict(value = "allTenants", allEntries = true)
    public void updateTenantStatus(Long tenantId, boolean isEnabled) {
        // Validazione input
        if (tenantId == null || tenantId <= 0) {
            log.warn("Invalid tenantId provided: {}", tenantId);
            throw new IllegalArgumentException("tenantId must be positive");
        }
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.error("Tenant not found: id={}", tenantId);
                    return new EntityNotFoundException("Tenant not found with id: " + tenantId);
                });
        
        // Audit: Log il cambio di stato
        boolean previousState = tenant.isEnabled();
        if (previousState != isEnabled) {
            log.info("AUDIT: Tenant status changed - tenantId={}, previousState={}, newState={}, timestamp={}",
                    tenantId, previousState, isEnabled, OffsetDateTime.now());
        }
        
        tenant.setEnabled(isEnabled);
        if (isEnabled) {
            tenant.setStatus("ACTIVE");
            if (tenant.getActivationDate() == null) {
                tenant.setActivationDate(OffsetDateTime.now());
            }
            if (tenant.getApprovedAt() == null) {
                tenant.setApprovedAt(OffsetDateTime.now());
            }
        }
        tenantRepository.save(tenant);
        log.debug("Tenant status updated successfully: tenantId={}, enabled={}", tenantId, isEnabled);
    }

    private TenantSummaryDto toDto(Tenant tenant) {
        return TenantSummaryDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .enabled(tenant.isEnabled())
                .build();
    }

    /**
     * Recupera i dettagli completi di un tenant per ID, incluso il contatto principale.
     * @param id ID del tenant
     * @return TenantDetailDto con tutti i campi
     * @throws EntityNotFoundException se il tenant non esiste
     */
    @Transactional(readOnly = true)
    public TenantDetailDto getTenantById(Long id) {
        if (id == null || id <= 0) {
            log.warn("Invalid tenantId provided: {}", id);
            throw new IllegalArgumentException("tenantId must be positive");
        }

        log.debug("Fetching tenant detail by id: {}", id);
        Tenant t = tenantRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tenant not found: id={}", id);
                    return new EntityNotFoundException("Tenant not found with id: " + id);
                });

        StaffUser contact = staffUserRepository.findFirstByTenantIdOrderByIdAsc(id).orElse(null);
        TenantSubscription sub = subscriptionRepository.findCurrentSubscriptionByTenantId(id).orElse(null);

        return TenantDetailDto.builder()
                .id(t.getId())
                .name(t.getName())
                .slug(t.getSlug())
                .subdomain(t.getSubdomain())
                .enabled(t.isEnabled())
                .legalName(t.getLegalName())
                .businessType(t.getBusinessType())
                .businessEmail(t.getBusinessEmail())
                .businessPhone(t.getBusinessPhone())
                .vatNumber(t.getVatNumber())
                .addressLine1(t.getAddressLine1())
                .addressLine2(t.getAddressLine2())
                .city(t.getCity())
                .province(t.getProvince())
                .postalCode(t.getPostalCode())
                .country(t.getCountry())
                .contactFirstName(contact != null ? contact.getFirstName() : null)
                .contactLastName(contact != null ? contact.getLastName() : null)
                .contactEmail(contact != null ? contact.getEmail() : null)
                .contactPhone(contact != null ? contact.getPhone() : null)
                .subscriptionPlan(sub != null && sub.getSubscriptionPlan() != null ? sub.getSubscriptionPlan().getCode() : null)
                .subscriptionStartDate(sub != null && sub.getActivatedAt() != null ? sub.getActivatedAt().format(DATE_FMT) : null)
                .subscriptionEndDate(sub != null && sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd().format(DATE_FMT) : null)
                .subscriptionStatus(sub != null ? sub.getStatus() : null)
                .subscriptionPaymentStatus(sub != null ? sub.getPaymentStatus() : null)
                .cancelAtPeriodEnd(sub != null && sub.isCancelAtPeriodEnd())
                .paymentMethod(sub != null ? sub.getPaymentMethod() : null)
                .build();
    }

    /**
     * Rinnovo manuale per tenant BANK_TRANSFER: riattiva tenant e imposta nuovo periodo.
     */
    @Transactional
    @CacheEvict(value = "allTenants", allEntries = true)
    public void renewManually(Long tenantId, String billingCycle) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        tenant.setStatus("ACTIVE");
        tenant.setEnabled(true);
        tenant.setUpdatedAt(OffsetDateTime.now());
        tenantRepository.save(tenant);

        TenantSubscription sub = subscriptionRepository.findCurrentSubscriptionByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found for tenant: " + tenantId));
        String cycle = "YEARLY".equalsIgnoreCase(billingCycle) ? "YEARLY" : "MONTHLY";
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime periodEnd = "YEARLY".equals(cycle) ? now.plusYears(1) : now.plusMonths(1);
        sub.setStatus("ACTIVE");
        sub.setPaymentStatus("PENDING");
        sub.setTrialEndsAt(null);
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(periodEnd);
        sub.setBillingCycle(cycle);
        subscriptionRepository.save(sub);
        log.info("BANK_TRANSFER tenant {} manually renewed ({}) until {}", tenantId, cycle, periodEnd);
    }
}

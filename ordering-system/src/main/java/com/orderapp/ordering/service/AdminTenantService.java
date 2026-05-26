package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.TenantRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private static final int DEFAULT_PAGE_SIZE = 20;

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
     * Recupera un tenant per ID.
     * @param id ID del tenant
     * @return Tenant entity
     * @throws EntityNotFoundException se il tenant non esiste
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tenantById", key = "#id")
    public Tenant getTenantById(Long id) {
        if (id == null || id <= 0) {
            log.warn("Invalid tenantId provided: {}", id);
            throw new IllegalArgumentException("tenantId must be positive");
        }
        
        log.debug("Fetching tenant by id: {}", id);
        return tenantRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tenant not found: id={}", id);
                    return new EntityNotFoundException("Tenant not found with id: " + id);
                });
    }
}

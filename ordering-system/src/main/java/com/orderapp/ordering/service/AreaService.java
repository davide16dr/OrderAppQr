package com.orderapp.ordering.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderapp.ordering.dto.AreaCreateRequest;
import com.orderapp.ordering.dto.AreaResponse;
import com.orderapp.ordering.dto.AreaUpdateRequest;
import com.orderapp.ordering.entity.AreaEntity;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.AreaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaService {
    private final AreaRepository areaRepository;

    /**
     * Recupera tutte le aree del tenant con cache.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tenantAreas", key = "#tenantId")
    public List<AreaResponse> listAreasByTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            log.warn("Invalid tenantId provided: {}", tenantId);
            throw new IllegalArgumentException("tenantId must be positive");
        }
        log.debug("Fetching areas for tenant: {}", tenantId);
        List<AreaResponse> areas = areaRepository.findByTenantIdOrderByDisplayOrderAscNameAsc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("Retrieved {} areas for tenant: {}", areas.size(), tenantId);
        return areas;
    }

    /**
     * Recupera un'area specifica del tenant con cache.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tenantArea", key = "#tenantId + '-' + #areaId")
    public AreaResponse getAreaById(Long tenantId, Long areaId) {
        if (tenantId == null || tenantId <= 0 || areaId == null || areaId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, areaId: {}", tenantId, areaId);
            throw new IllegalArgumentException("tenantId and areaId must be positive");
        }
        log.debug("Fetching area - tenantId: {}, areaId: {}", tenantId, areaId);
        AreaEntity area = areaRepository.findByTenantIdAndId(tenantId, areaId)
                .orElseThrow(() -> {
                    log.error("Area not found - tenantId: {}, areaId: {}", tenantId, areaId);
                    return new ResourceNotFoundException("Area non trovata");
                });
        return toResponse(area);
    }

    /**
     * Crea una nuova area per il tenant.
     */
    @Transactional
    @CacheEvict(value = "tenantAreas", key = "#tenantId", allEntries = false)
    public AreaResponse createArea(Long tenantId, AreaCreateRequest request) {
        // Validazione input
        if (tenantId == null || tenantId <= 0) {
            log.warn("Invalid tenantId provided: {}", tenantId);
            throw new IllegalArgumentException("tenantId must be positive");
        }
        
        if (request.name() == null || request.name().trim().isEmpty()) {
            log.warn("Empty area name provided for tenantId: {}", tenantId);
            throw new BusinessException("Il nome dell'area è obbligatorio");
        }
        
        String trimmedName = request.name().trim();
        if (areaRepository.existsByTenantIdAndNameIgnoreCase(tenantId, trimmedName)) {
            log.warn("Duplicate area name - tenantId: {}, name: {}", tenantId, trimmedName);
            throw new BusinessException("Area già esistente");
        }

        log.info("Creating new area - tenantId: {}, name: {}", tenantId, trimmedName);
        OffsetDateTime now = OffsetDateTime.now();
        AreaEntity entity = AreaEntity.builder()
                .tenantId(tenantId)
                .name(trimmedName)
                .description(request.description())
                .displayOrder(0)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(areaRepository.save(entity));
    }

    /**
     * Aggiorna un'area esistente.
     */
    @Transactional
    @CacheEvict(value = {"tenantAreas", "tenantArea"}, allEntries = true)
    public AreaResponse updateArea(Long tenantId, Long areaId, AreaUpdateRequest request) {
        // Validazione input
        if (tenantId == null || tenantId <= 0 || areaId == null || areaId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, areaId: {}", tenantId, areaId);
            throw new IllegalArgumentException("tenantId and areaId must be positive");
        }
        
        if (request.name() == null || request.name().trim().isEmpty()) {
            log.warn("Empty area name provided for update - tenantId: {}, areaId: {}", tenantId, areaId);
            throw new BusinessException("Il nome dell'area è obbligatorio");
        }
        
        AreaEntity area = areaRepository.findByTenantIdAndId(tenantId, areaId)
                .orElseThrow(() -> {
                    log.error("Area not found for update - tenantId: {}, areaId: {}", tenantId, areaId);
                    return new ResourceNotFoundException("Area non trovata");
                });

        String trimmedName = request.name().trim();
        if (!area.getName().equalsIgnoreCase(trimmedName)
                && areaRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, trimmedName, areaId)) {
            log.warn("Duplicate area name on update - tenantId: {}, name: {}", tenantId, trimmedName);
            throw new BusinessException("Area già esistente");
        }

        log.info("Updating area - tenantId: {}, areaId: {}", tenantId, areaId);
        area.setName(trimmedName);
        area.setDescription(request.description());
        if (request.active() != null) {
            area.setActive(request.active());
        }
        area.setUpdatedAt(OffsetDateTime.now());

        return toResponse(areaRepository.save(area));
    }

    /**
     * Elimina (soft delete) un'area dal tenant.
     */
    @Transactional
    @CacheEvict(value = {"tenantAreas", "tenantArea"}, allEntries = true)
    public void deleteArea(Long tenantId, Long areaId) {
        if (tenantId == null || tenantId <= 0 || areaId == null || areaId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, areaId: {}", tenantId, areaId);
            throw new IllegalArgumentException("tenantId and areaId must be positive");
        }
        
        AreaEntity area = areaRepository.findByTenantIdAndId(tenantId, areaId)
                .orElseThrow(() -> {
                    log.error("Area not found for deletion - tenantId: {}, areaId: {}", tenantId, areaId);
                    return new ResourceNotFoundException("Area non trovata");
                });

        log.info("Deleting (soft delete) area - tenantId: {}, areaId: {}", tenantId, areaId);
        area.setActive(false);
        area.setUpdatedAt(OffsetDateTime.now());
        areaRepository.save(area);
    }

    /**
     * Recupera un'area richiesta o lancia eccezione.
     */
    @Transactional(readOnly = true)
    public AreaEntity requireTenantArea(Long tenantId, Long areaId) {
        if (tenantId == null || tenantId <= 0 || areaId == null || areaId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, areaId: {}", tenantId, areaId);
            throw new IllegalArgumentException("tenantId and areaId must be positive");
        }
        
        log.debug("Requiring area - tenantId: {}, areaId: {}", tenantId, areaId);
        return areaRepository.findByTenantIdAndId(tenantId, areaId)
                .orElseThrow(() -> {
                    log.error("Required area not found - tenantId: {}, areaId: {}", tenantId, areaId);
                    return new ResourceNotFoundException("Area non trovata");
                });
    }

    private AreaResponse toResponse(AreaEntity area) {
        return new AreaResponse(
                area.getId(),
                area.getTenantId(),
                area.getName(),
                area.getDescription(),
                area.isActive(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }
}
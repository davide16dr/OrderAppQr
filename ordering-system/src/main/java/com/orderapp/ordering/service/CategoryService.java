package com.orderapp.ordering.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import com.orderapp.ordering.dto.CreateTenantCategoryRequestDto;
import com.orderapp.ordering.dto.TenantCategoryDto;
import com.orderapp.ordering.dto.UpdateTenantCategoryRequestDto;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.CategoryRepository;

import java.util.List;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Recupera tutte le categorie del tenant con cache.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tenantCategories", key = "#tenantId")
    public List<TenantCategoryDto> getTenantCategories(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            log.warn("Invalid tenantId provided: {}", tenantId);
            throw new IllegalArgumentException("tenantId must be positive");
        }
        log.debug("Fetching categories for tenant: {}", tenantId);
        List<TenantCategoryDto> categories = categoryRepository.getTenantCategories(tenantId);
        log.info("Retrieved {} categories for tenant: {}", categories.size(), tenantId);
        return categories;
    }

    /**
     * Recupera una categoria specifica del tenant con cache.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tenantCategory", key = "#tenantId + '-' + #categoryId")
    public TenantCategoryDto getTenantCategoryById(Long tenantId, Long categoryId) {
        if (tenantId == null || tenantId <= 0 || categoryId == null || categoryId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, categoryId: {}", tenantId, categoryId);
            throw new IllegalArgumentException("tenantId and categoryId must be positive");
        }
        log.debug("Fetching category - tenantId: {}, categoryId: {}", tenantId, categoryId);
        return categoryRepository.getTenantCategoryById(tenantId, categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found - tenantId: {}, categoryId: {}", tenantId, categoryId);
                    return new ResourceNotFoundException("Categoria non trovata");
                });
    }

    /**
     * Crea una nuova categoria per il tenant.
     */
    @Transactional
    @CacheEvict(value = "tenantCategories", key = "#tenantId", allEntries = false)
    public TenantCategoryDto createTenantCategory(Long tenantId, CreateTenantCategoryRequestDto request) {
        // Validazione input
        if (tenantId == null || tenantId <= 0) {
            log.warn("Invalid tenantId provided: {}", tenantId);
            throw new IllegalArgumentException("tenantId must be positive");
        }
        
        if (request.name() == null || request.name().trim().isEmpty()) {
            log.warn("Empty category name provided for tenantId: {}", tenantId);
            throw new BusinessException("Il nome della categoria è obbligatorio");
        }

        String trimmedName = request.name().trim();

        if (categoryRepository.categoryNameExists(tenantId, trimmedName)) {
            log.warn("Duplicate category name - tenantId: {}, name: {}", tenantId, trimmedName);
            throw new BusinessException("Una categoria con questo nome esiste già");
        }

        log.info("Creating new category - tenantId: {}, name: {}", tenantId, trimmedName);
        return categoryRepository.createTenantCategory(
                tenantId,
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                request.displayOrder() != null ? request.displayOrder() : 0
        );
    }

    /**
     * Aggiorna una categoria esistente.
     */
    @Transactional
    @CacheEvict(value = {"tenantCategories", "tenantCategory"}, allEntries = true)
    public TenantCategoryDto updateTenantCategory(Long tenantId, Long categoryId,
            UpdateTenantCategoryRequestDto request) {
        // Validazione input
        if (tenantId == null || tenantId <= 0 || categoryId == null || categoryId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, categoryId: {}", tenantId, categoryId);
            throw new IllegalArgumentException("tenantId and categoryId must be positive");
        }
        
        // Verifica che la categoria esista
        getTenantCategoryById(tenantId, categoryId);

        // Se il nome viene modificato, controlla che non esista un'altra categoria con lo stesso nome
        if (request.name() != null && !request.name().trim().isEmpty()) {
            String trimmedName = request.name().trim();
            if (categoryRepository.categoryNameExistsExcluding(tenantId, trimmedName, categoryId)) {
                log.warn("Duplicate category name on update - tenantId: {}, name: {}", tenantId, trimmedName);
                throw new BusinessException("Una categoria con questo nome esiste già");
            }
        }

        log.info("Updating category - tenantId: {}, categoryId: {}", tenantId, categoryId);
        return categoryRepository.updateTenantCategory(
                tenantId,
                categoryId,
                request.name() != null ? request.name().trim() : null,
                request.description() != null ? request.description().trim() : null,
                request.displayOrder(),
                request.status()
        );
    }

    /**
     * Elimina una categoria dal tenant.
     */
    @Transactional
    @CacheEvict(value = {"tenantCategories", "tenantCategory"}, allEntries = true)
    public void deleteTenantCategory(Long tenantId, Long categoryId) {
        if (tenantId == null || tenantId <= 0 || categoryId == null || categoryId <= 0) {
            log.warn("Invalid ids provided - tenantId: {}, categoryId: {}", tenantId, categoryId);
            throw new IllegalArgumentException("tenantId and categoryId must be positive");
        }
        log.info("Deleting category - tenantId: {}, categoryId: {}", tenantId, categoryId);
        // Verifica che la categoria esista
        getTenantCategoryById(tenantId, categoryId);

        boolean deleted = categoryRepository.deleteTenantCategory(tenantId, categoryId);
        if (!deleted) {
            throw new BusinessException("Impossibile eliminare la categoria");
        }
    }
}

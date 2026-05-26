package com.orderapp.ordering.service;

import com.orderapp.ordering.dto.CreateTenantCategoryRequestDto;
import com.orderapp.ordering.dto.TenantCategoryDto;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Async wrapper per CategoryService
 * 
 * Permette di eseguire operazioni lunghe in background
 * Utile per batch operations, export, report generation
 */
@Slf4j
@Service
public class AsyncCategoryService {

    private final CategoryService categoryService;

    public AsyncCategoryService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Recupera categorie in modo asincrono
     * Ideale per UI che aspetta il result
     */
    @Async("taskExecutor")
    public CompletableFuture<List<TenantCategoryDto>> getTenantCategoriesAsync(Long tenantId) {
        log.info("Starting async category fetch for tenant: {}", tenantId);
        try {
            List<TenantCategoryDto> categories = categoryService.getTenantCategories(tenantId);
            log.info("Async category fetch completed - retrieved {} categories", categories.size());
            return CompletableFuture.completedFuture(categories);
        } catch (Exception e) {
            log.error("Async category fetch failed for tenant: {}", tenantId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Crea categoria in modo asincrono
     * Ritorna il result quando completato
     */
    @Async("taskExecutor")
    public CompletableFuture<TenantCategoryDto> createTenantCategoryAsync(
            Long tenantId, 
            CreateTenantCategoryRequestDto request) {
        
        log.info("Starting async category creation for tenant: {}", tenantId);
        try {
            TenantCategoryDto category = categoryService.createTenantCategory(tenantId, request);
            log.info("Async category creation completed");
            return CompletableFuture.completedFuture(category);
        } catch (BusinessException e) {
            log.warn("Business error during async category creation: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Async category creation failed for tenant: {}", tenantId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Esporta tutte le categorie di un tenant
     * Operazione potenzialmente lunga, ideale per async
     */
    @Async("taskExecutor")
    public CompletableFuture<String> exportCategoriesAsync(Long tenantId) {
        log.info("Starting async category export for tenant: {}", tenantId);
        try {
            List<TenantCategoryDto> categories = categoryService.getTenantCategories(tenantId);
            
            // Simula processing lungo
            Thread.sleep(1000);
            
            // Buildexport string (CSV, JSON, etc)
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Nome,Descrizione,DisplayOrder,Status\n");
            
            categories.forEach(cat -> csv.append(String.format("%d,%s,%s,%d,%s\n",
                    cat.id(), cat.name(), cat.description() != null ? cat.description() : "",
                    cat.displayOrder(), cat.status())));
            
            log.info("Async category export completed - {} categories exported", categories.size());
            return CompletableFuture.completedFuture(csv.toString());
        } catch (InterruptedException e) {
            log.error("Async category export interrupted", e);
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Async category export failed for tenant: {}", tenantId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Fire-and-forget per operazioni che non richiede il result
     * Es: log analitici, trigger webhook
     */
    @Async("taskExecutor")
    public void notifyExternalSystemAsync(Long tenantId, String categoryName) {
        log.info("Sending async notification for new category - tenantId: {}, name: {}", tenantId, categoryName);
        try {
            // Simula chiamata a sistema esterno
            Thread.sleep(500);
            log.info("External notification sent successfully");
        } catch (InterruptedException e) {
            log.error("External notification interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to send external notification", e);
            // Non propagare l'errore - fire-and-forget
        }
    }
}

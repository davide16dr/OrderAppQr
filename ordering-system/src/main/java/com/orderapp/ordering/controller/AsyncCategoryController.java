package com.orderapp.ordering.controller;

import com.orderapp.ordering.dto.CreateTenantCategoryRequestDto;
import com.orderapp.ordering.dto.TenantCategoryDto;
import com.orderapp.ordering.service.AsyncCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Async Category Controller
 * 
 * Endpoints asincroni per operazioni lunghe
 * Ideale per UI che aspetta il result senza bloccare
 */
@Slf4j
@RestController
@RequestMapping("/api/async/categories")
@RequiredArgsConstructor
@Tag(name = "Categories (Async)", description = "Asynchronous category endpoints")
@SecurityRequirement(name = "Bearer Token")
public class AsyncCategoryController {

    private final AsyncCategoryService asyncCategoryService;

    @GetMapping("/{tenantId}")
    @Operation(
        summary = "Get tenant categories (async)",
        description = "Recupera categorie del tenant in modo asincrono"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Categories retrieved successfully",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public CompletableFuture<ResponseEntity<List<TenantCategoryDto>>> getCategoriesAsync(
            @Parameter(description = "Tenant ID", required = true)
            @PathVariable Long tenantId) {
        
        log.info("Async request for categories - tenantId: {}", tenantId);
        
        return asyncCategoryService.getTenantCategoriesAsync(tenantId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Async category fetch failed", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/{tenantId}")
    @Operation(
        summary = "Create category (async)",
        description = "Crea una nuova categoria in modo asincrono"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Category created successfully",
            content = @Content(schema = @Schema(implementation = TenantCategoryDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public CompletableFuture<ResponseEntity<TenantCategoryDto>> createCategoryAsync(
            @Parameter(description = "Tenant ID", required = true)
            @PathVariable Long tenantId,
            @RequestBody CreateTenantCategoryRequestDto request) {
        
        log.info("Async request to create category - tenantId: {}, name: {}", tenantId, request.name());
        
        return asyncCategoryService.createTenantCategoryAsync(tenantId, request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Async category creation failed", ex);
                    return ResponseEntity.badRequest().build();
                });
    }

    @GetMapping("/{tenantId}/export")
    @Operation(
        summary = "Export categories (async)",
        description = "Esporta tutte le categorie in formato CSV (async)"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Export completed successfully",
        content = @Content(schema = @Schema(implementation = String.class))
    )
    public CompletableFuture<ResponseEntity<String>> exportCategoriesAsync(
            @Parameter(description = "Tenant ID", required = true)
            @PathVariable Long tenantId) {
        
        log.info("Async request to export categories - tenantId: {}", tenantId);
        
        return asyncCategoryService.exportCategoriesAsync(tenantId)
                .thenApply(csv -> ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=categories.csv")
                        .body(csv))
                .exceptionally(ex -> {
                    log.error("Async category export failed", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }
}

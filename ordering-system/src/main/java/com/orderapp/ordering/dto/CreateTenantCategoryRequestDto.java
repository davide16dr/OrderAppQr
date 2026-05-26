package com.orderapp.ordering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO per creazione categoria tenant
 */
@Schema(description = "Richiesta di creazione categoria")
public record CreateTenantCategoryRequestDto(
    @NotBlank(message = "Nome categoria obbligatorio")
    @Size(min = 2, max = 100, message = "Nome deve essere tra 2 e 100 caratteri")
    @Schema(description = "Nome della categoria", example = "Bevande")
    String name,
    
    @Size(max = 500, message = "Descrizione massimo 500 caratteri")
    @Schema(description = "Descrizione della categoria", example = "Bevande varie e cocktail")
    String description,
    
    @Min(value = 0, message = "Display order deve essere >= 0")
    @Schema(description = "Ordine di visualizzazione", example = "0")
    Integer displayOrder
) {}

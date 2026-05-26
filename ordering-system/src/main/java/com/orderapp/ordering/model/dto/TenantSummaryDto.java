package com.orderapp.ordering.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantSummaryDto {
    private Long id;
    private String name;
    private String slug;
    private boolean enabled;
}

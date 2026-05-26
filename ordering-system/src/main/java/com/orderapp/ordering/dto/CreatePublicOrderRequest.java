package com.orderapp.ordering.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePublicOrderRequest {
    String token;
    String tenant;
    String location;
    String customerNote;
    List<CreatePublicOrderLineRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePublicOrderLineRequest {
        Long tenantProductId;
        String productName;
        Integer quantity;
        Long unitPriceCents;
        List<Long> selectedModifierOptionIds;
    }
}

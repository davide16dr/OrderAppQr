package com.orderapp.ordering.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePublicOrderResponse {
    Long id;
    String status;
    BigDecimal totalAmount;
    List<OrderItemLine> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemLine {
        String productName;
        Integer quantity;
        BigDecimal unitPrice;
        BigDecimal lineTotal;
    }
}

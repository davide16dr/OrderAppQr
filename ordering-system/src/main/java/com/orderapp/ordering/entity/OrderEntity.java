package com.orderapp.ordering.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "location_label_snapshot", nullable = false, length = 150)
    private String locationLabelSnapshot;

    @Column(name = "area_name_snapshot", length = 150)
    private String areNameSnapshot;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "internal_note", length = 500)
    private String internalNote;

    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "created_by_staff_id")
    private Long createdByStaffId;

    @Column(name = "accepted_by_staff_id")
    private Long acceptedByStaffId;

    @Column(name = "delivered_by_staff_id")
    private Long deliveredByStaffId;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

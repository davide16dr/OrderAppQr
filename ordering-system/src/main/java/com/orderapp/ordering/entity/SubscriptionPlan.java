package com.orderapp.ordering.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {
    @Id
    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "price_monthly", precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", precision = 10, scale = 2)
    private BigDecimal priceYearly;

    @Column(name = "max_locations")
    private Integer maxLocations;

    @Column(name = "max_staff_users")
    private Integer maxStaffUsers;

    @Column(name = "max_products")
    private Integer maxProducts;

    @Column(name = "qr_batch_enabled", nullable = false)
    @Builder.Default
    private Boolean qrBatchEnabled = false;

    @Column(name = "realtime_dashboard", nullable = false)
    @Builder.Default
    private Boolean realtimeDashboard = true;

    @Column(name = "global_catalog_enabled", nullable = false)
    @Builder.Default
    private Boolean globalCatalogEnabled = true;

    @Column(name = "stripe_price_id_monthly", length = 100)
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_yearly", length = 100)
    private String stripePriceIdYearly;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

package com.orderapp.ordering.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "business_registration_requests")
public class BusinessRegistrationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_slug", nullable = false)
    private String requestedSlug;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "vat_number")
    private String vatNumber;

    @Column(name = "business_email", nullable = false)
    private String businessEmail;

    @Column(name = "business_phone")
    private String businessPhone;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "contact_first_name", nullable = false)
    private String contactFirstName;

    @Column(name = "contact_last_name", nullable = false)
    private String contactLastName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "requested_plan_code")
    private String requestedPlanCode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by_staff_user_id")
    private Long reviewedByStaffUserId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

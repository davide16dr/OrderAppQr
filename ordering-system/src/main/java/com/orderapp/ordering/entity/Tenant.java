package com.orderapp.ordering.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
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
@Table(name = "tenants")
public class Tenant {
	@Id
	@SequenceGenerator(name = "tenants_id_seq_gen", sequenceName = "tenants_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenants_id_seq_gen")
	private Long id;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(nullable = false, unique = true)
	private String subdomain;

	@Column(nullable = false)
	private String name;

	@Column(name = "legal_name")
	private String legalName;

	@Column(name = "business_type", nullable = false)
	private String businessType;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private String timezone;

	@Column(name = "currency_code", nullable = false)
	private String currencyCode;

	@Column(name = "vat_number")
	private String vatNumber;

	@Column(name = "business_email")
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

	@Column(nullable = false)
	private String country;

	@Column(name = "registration_source", nullable = false)
	private String registrationSource;

	@Column(name = "activation_date")
	private OffsetDateTime activationDate;

	@Column(name = "approved_at")
	private OffsetDateTime approvedAt;

	@Column(name = "approved_by_staff_user_id")
	private Long approvedByStaffUserId;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "is_demo", nullable = false)
	private boolean isDemo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "branding_json", columnDefinition = "jsonb")
	private String brandingJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "opening_config_json", columnDefinition = "jsonb")
	private String openingConfigJson;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}

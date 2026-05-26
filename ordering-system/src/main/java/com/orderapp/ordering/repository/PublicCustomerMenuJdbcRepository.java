package com.orderapp.ordering.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PublicCustomerMenuJdbcRepository {
	private final NamedParameterJdbcTemplate jdbc;

	public List<OrderableProductRow> findOrderableProductsByIds(long tenantId, List<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return List.of();
		}
		String sql = """
			select
				tp.id as product_id,
				tp.name as product_name,
				tp.price as price,
				tp.department as department
			from tenant_products tp
			where tp.tenant_id = :tenantId
				and tp.id in (:productIds)
				and tp.status = 'ACTIVE'
				and tp.available_for_order = true
		""";
		return jdbc.query(sql, Map.of("tenantId", tenantId, "productIds", productIds), ORDERABLE_PRODUCT_MAPPER);
	}

	public List<ModifierOptionForOrderRow> findModifierOptionsForOrder(long tenantId, List<Long> optionIds) {
		if (optionIds == null || optionIds.isEmpty()) {
			return List.of();
		}
		String sql = """
			select
				tpmg.tenant_product_id,
				mg.id as group_id,
				mg.name as group_name,
				mo.id as option_id,
				mo.name as option_name,
				mo.price_delta
			from modifier_options mo
			join modifier_groups mg on mg.id = mo.modifier_group_id
			join tenant_product_modifier_groups tpmg on tpmg.modifier_group_id = mg.id
			where mo.id in (:optionIds)
				and mg.tenant_id = :tenantId
				and mg.status = 'ACTIVE'
				and mo.status = 'ACTIVE'
			order by tpmg.tenant_product_id, mg.id, mo.id
		""";
		return jdbc.query(sql, Map.of("tenantId", tenantId, "optionIds", optionIds), MODIFIER_OPTION_FOR_ORDER_MAPPER);
	}

	public Optional<ResolvedContextRow> findResolvedContextByToken(String token) {
		String sql = """
			select
				t.id as tenant_id,
				t.name as tenant_name,
				l.label as location_label,
				a.name as area_name,
				l.status as location_status
			from location_tokens lt
			join tenants t on t.id = lt.tenant_id
			join locations l on l.id = lt.location_id
			left join areas a on a.id = l.area_id
			where lt.token = :token
				and lt.status = 'ACTIVE'
				and (lt.expires_at is null or lt.expires_at > now())
			limit 1
		""";

		List<ResolvedContextRow> rows = jdbc.query(sql, Map.of("token", token), RESOLVED_CONTEXT_MAPPER);
		return rows.stream().findFirst();
	}

	public Optional<LocationRow> findLocationByLabel(long tenantId, String locationLabel) {
		String sql = """
			select
				l.id as location_id,
				l.label as location_label,
				a.name as area_name,
				l.status as location_status
			from locations l
			left join areas a on a.id = l.area_id
			where l.tenant_id = :tenantId
				and lower(l.label) = lower(:label)
			limit 1
		""";

		List<LocationRow> rows = jdbc.query(sql, Map.of("tenantId", tenantId, "label", locationLabel), LOCATION_MAPPER);
		return rows.stream().findFirst();
	}

	public Optional<LocationRow> findFirstActiveLocation(long tenantId) {
		String sql = """
			select
				l.id as location_id,
				l.label as location_label,
				a.name as area_name,
				l.status as location_status
			from locations l
			left join areas a on a.id = l.area_id
			where l.tenant_id = :tenantId
				and l.status = 'ACTIVE'
			order by l.id
			limit 1
		""";

		List<LocationRow> rows = jdbc.query(sql, Map.of("tenantId", tenantId), LOCATION_MAPPER);
		return rows.stream().findFirst();
	}

	public List<CategoryRow> findActiveCategories(long tenantId) {
		String sql = """
			select c.id, c.name
			from categories c
			where c.tenant_id = :tenantId
				and c.status = 'ACTIVE'
			order by c.display_order, c.id
		""";

		return jdbc.query(sql, Map.of("tenantId", tenantId), CATEGORY_MAPPER);
	}

	public List<ProductRow> findActiveProductsWithPrimaryCategory(long tenantId) {
		String sql = """
			select
				p.product_id,
				p.product_name,
				p.product_description,
				p.price,
				p.image_url,
				p.category_id,
				p.metadata_json
			from (
				select
					tp.id as product_id,
					tp.name as product_name,
					tp.description as product_description,
					tp.price as price,
					tp.image_url as image_url,
					c.id as category_id,
					tp.metadata_json as metadata_json,
					row_number() over (
						partition by tp.id
						order by ctp.display_order, c.display_order, c.id
					) as rn,
					c.display_order as category_display_order,
					ctp.display_order as product_display_order
				from tenant_products tp
				join category_tenant_products ctp on ctp.tenant_product_id = tp.id
				join categories c on c.id = ctp.category_id
				where tp.tenant_id = :tenantId
					and tp.status = 'ACTIVE'
					and tp.available_for_order = true
					and c.tenant_id = :tenantId
					and c.status = 'ACTIVE'
			) p
			where p.rn = 1
			order by p.category_display_order, p.product_display_order, p.product_name, p.product_id
		""";

		return jdbc.query(sql, Map.of("tenantId", tenantId), PRODUCT_MAPPER);
	}

	public Optional<ProductRow> findProductById(long tenantId, long productId) {
		String sql = """
			select
				tp.id as product_id,
				tp.name as product_name,
				tp.description as product_description,
				tp.price as price,
				tp.image_url as image_url,
				c.id as category_id,
				tp.metadata_json as metadata_json
			from tenant_products tp
			left join category_tenant_products ctp on ctp.tenant_product_id = tp.id
			left join categories c on c.id = ctp.category_id
			where tp.tenant_id = :tenantId
				and tp.id = :productId
			limit 1
		""";

		List<ProductRow> rows = jdbc.query(sql, Map.of("tenantId", tenantId, "productId", productId), PRODUCT_MAPPER);
		return rows.stream().findFirst();
	}

	public List<ModifierGroupRow> findModifierGroupsForProducts(long tenantId, List<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return List.of();
		}
		String sql = """
			select
				tpmg.tenant_product_id,
				mg.id as group_id,
				mg.name as group_name,
				mg.min_selectable,
				mg.max_selectable,
				mg.required,
				mo.id as option_id,
				mo.name as option_name,
				null as price,
				mo.price_delta
			from tenant_product_modifier_groups tpmg
			join modifier_groups mg on mg.id = tpmg.modifier_group_id
			join modifier_options mo on mo.modifier_group_id = mg.id
			where tpmg.tenant_product_id in (:productIds)
				and mg.tenant_id = :tenantId
				and mg.status = 'ACTIVE'
				and mo.status = 'ACTIVE'
			order by tpmg.tenant_product_id, mg.id, mo.id
		""";
		return jdbc.query(sql, Map.of("tenantId", tenantId, "productIds", productIds), MODIFIER_GROUP_MAPPER);
	}

	public record ResolvedContextRow(
		long tenantId,
		String tenantName,
		String locationLabel,
		String areaName,
		String locationStatus
	) {}

	public record LocationRow(
		long locationId,
		String locationLabel,
		String areaName,
		String locationStatus
	) {}

	public record CategoryRow(
		long id,
		String name
	) {}

	public record ProductRow(
		long id,
		String name,
		String description,
		BigDecimal price,
		String imageUrl,
		long categoryId,
		String metadataJson
	) {}

	public record ModifierGroupRow(
		long productId,
		long groupId,
		String groupName,
		int minSelectable,
		Integer maxSelectable,
		boolean required,
		long optionId,
		String optionName,
		BigDecimal price,
		BigDecimal priceDelta
	) {
		public Integer priceCents() {
			if (price == null) return null;
			return price.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValue();
		}

		public Integer priceDeltaCents() {
			if (priceDelta == null) return null;
			return priceDelta.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValue();
		}
	}

	public record OrderableProductRow(
		long id,
		String name,
		BigDecimal price,
		String department
	) {
		public int priceCents() {
			if (price == null) return 0;
			return price.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValue();
		}
	}

	public record ModifierOptionForOrderRow(
		long tenantProductId,
		long groupId,
		String groupName,
		long optionId,
		String optionName,
		BigDecimal priceDelta
	) {
		public int priceDeltaCents() {
			if (priceDelta == null) return 0;
			return priceDelta.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValue();
		}
	}

	private static final RowMapper<ResolvedContextRow> RESOLVED_CONTEXT_MAPPER = (rs, rowNum) -> new ResolvedContextRow(
		rs.getLong("tenant_id"),
		rs.getString("tenant_name"),
		rs.getString("location_label"),
		rs.getString("area_name"),
		rs.getString("location_status")
	);

	private static final RowMapper<LocationRow> LOCATION_MAPPER = (rs, rowNum) -> new LocationRow(
		rs.getLong("location_id"),
		rs.getString("location_label"),
		rs.getString("area_name"),
		rs.getString("location_status")
	);

	private static final RowMapper<CategoryRow> CATEGORY_MAPPER = (rs, rowNum) -> new CategoryRow(
		rs.getLong("id"),
		rs.getString("name")
	);

	private static final RowMapper<ProductRow> PRODUCT_MAPPER = new RowMapper<>() {
		@Override
		public ProductRow mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new ProductRow(
				rs.getLong("product_id"),
				rs.getString("product_name"),
				rs.getString("product_description"),
				rs.getBigDecimal("price"),
				rs.getString("image_url"),
				rs.getLong("category_id"),
				rs.getString("metadata_json")
			);
		}
	};

	private static final RowMapper<OrderableProductRow> ORDERABLE_PRODUCT_MAPPER = new RowMapper<>() {
		@Override
		public OrderableProductRow mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new OrderableProductRow(
				rs.getLong("product_id"),
				rs.getString("product_name"),
				rs.getBigDecimal("price"),
				rs.getString("department")
			);
		}
	};

	private static final RowMapper<ModifierGroupRow> MODIFIER_GROUP_MAPPER = (rs, rowNum) -> {
		int maxSel = rs.getInt("max_selectable");
		Integer maxSelectable = rs.wasNull() ? null : maxSel;
		return new ModifierGroupRow(
			rs.getLong("tenant_product_id"),
			rs.getLong("group_id"),
			rs.getString("group_name"),
			rs.getInt("min_selectable"),
			maxSelectable,
			rs.getBoolean("required"),
			rs.getLong("option_id"),
			rs.getString("option_name"),
			rs.getBigDecimal("price"),
			rs.getBigDecimal("price_delta")
		);
	};

	private static final RowMapper<ModifierOptionForOrderRow> MODIFIER_OPTION_FOR_ORDER_MAPPER = (rs, rowNum) -> new ModifierOptionForOrderRow(
		rs.getLong("tenant_product_id"),
		rs.getLong("group_id"),
		rs.getString("group_name"),
		rs.getLong("option_id"),
		rs.getString("option_name"),
		rs.getBigDecimal("price_delta")
	);
}

package com.orderapp.ordering.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderapp.ordering.dto.CreatePublicOrderRequest;
import com.orderapp.ordering.dto.CreatePublicOrderResponse;
import com.orderapp.ordering.entity.OrderEntity;
import com.orderapp.ordering.entity.OrderItemEntity;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.OrderItemRepository;
import com.orderapp.ordering.repository.OrderRepository;
import com.orderapp.ordering.repository.PublicCustomerMenuJdbcRepository;
import com.orderapp.ordering.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicOrderService {
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final TenantRepository tenantRepository;
	private final PublicCustomerMenuJdbcRepository menuJdbcRepository;
	private final ObjectMapper objectMapper;
	private final NamedParameterJdbcTemplate jdbc;
	private final OrderEventPublisher orderEventPublisher;

	@Transactional
	public CreatePublicOrderResponse createPublicOrder(CreatePublicOrderRequest request) {
		// Resolve tenant and location via token or params
		Tenant tenant;
		PublicCustomerMenuJdbcRepository.LocationRow location;

		if (request.getToken() != null && !request.getToken().isBlank()) {
			var resolved = menuJdbcRepository
				.findResolvedContextByToken(request.getToken().trim())
				.orElseThrow(() -> new ResourceNotFoundException("Token non valido o scaduto"));
			
			tenant = tenantRepository.findById(resolved.tenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Tenant non trovato"));
			
			location = menuJdbcRepository
				.findLocationByLabel(resolved.tenantId(), resolved.locationLabel())
				.or(() -> menuJdbcRepository.findFirstActiveLocation(resolved.tenantId()))
				.orElseThrow(() -> new ResourceNotFoundException("Location non trovata"));
		} else {
			String tenantParam = request.getTenant();
			if (tenantParam == null || tenantParam.isBlank()) {
				List<Tenant> activeTenants = tenantRepository.findAll().stream()
					.filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
					.toList();

				if (activeTenants.size() != 1) {
					throw new ResourceNotFoundException("Tenant non trovato");
				}

				tenant = activeTenants.get(0);
			} else {
				tenant = tenantRepository
					.findBySubdomainIgnoreCase(tenantParam)
					.or(() -> tenantRepository.findBySlugIgnoreCase(tenantParam))
					.filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
					.orElseThrow(() -> new ResourceNotFoundException("Tenant non trovato"));
			}
			
			if (request.getLocation() != null && !request.getLocation().isBlank()) {
				location = menuJdbcRepository
					.findLocationByLabel(tenant.getId(), request.getLocation().trim())
					.orElseThrow(() -> new ResourceNotFoundException("Location non trovata"));
			} else {
				location = menuJdbcRepository
					.findFirstActiveLocation(tenant.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Location non trovata"));
			}
		}

		if (!isOrderingEnabledByTenantSettings(tenant)) {
			throw new IllegalArgumentException("Le ordinazioni non sono disponibili in questo momento");
		}

		if (request.getItems() == null || request.getItems().isEmpty()) {
			throw new IllegalArgumentException("Nessun prodotto selezionato");
		}

		// Create order
		OffsetDateTime now = OffsetDateTime.now();

		List<CreatePublicOrderRequest.CreatePublicOrderLineRequest> requestLines = request.getItems().stream()
			.filter(Objects::nonNull)
			.toList();
		if (requestLines.isEmpty()) {
			throw new IllegalArgumentException("Nessun prodotto selezionato");
		}

		for (int i = 0; i < requestLines.size(); i++) {
			var line = requestLines.get(i);
			if (line.getTenantProductId() == null) {
				throw new IllegalArgumentException("tenantProductId mancante alla riga " + i);
			}
			if (line.getQuantity() == null || line.getQuantity() <= 0) {
				throw new IllegalArgumentException("quantity non valida alla riga " + i);
			}
		}

		List<Long> productIds = requestLines.stream().map(CreatePublicOrderRequest.CreatePublicOrderLineRequest::getTenantProductId).toList();
		Map<Long, PublicCustomerMenuJdbcRepository.OrderableProductRow> productsById = menuJdbcRepository
			.findOrderableProductsByIds(tenant.getId(), productIds)
			.stream()
			.collect(Collectors.toMap(PublicCustomerMenuJdbcRepository.OrderableProductRow::id, p -> p, (a, b) -> a));

		Set<Long> requestedOptionIdsSet = new HashSet<>();
		for (var line : requestLines) {
			if (line.getSelectedModifierOptionIds() != null) {
				requestedOptionIdsSet.addAll(line.getSelectedModifierOptionIds());
			}
		}
		List<Long> requestedOptionIds = new ArrayList<>(requestedOptionIdsSet);
		Map<Long, PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow> modifierOptionsById;
		if (requestedOptionIds.isEmpty()) {
			modifierOptionsById = Collections.emptyMap();
		} else {
			modifierOptionsById = menuJdbcRepository
				.findModifierOptionsForOrder(tenant.getId(), requestedOptionIds)
				.stream()
				.collect(Collectors.toMap(PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow::optionId, o -> o, (a, b) -> a));
		}

		// Handle synthetic modifier option ids generated from product metadata (negative ids)
		if (!requestedOptionIds.isEmpty()) {
			for (Long optId : requestedOptionIds) {
				if (modifierOptionsById.containsKey(optId)) continue;
				if (optId == null) continue;
				if (optId >= 0) continue; // only handle negatives
				// Negative ids encode product and index. Try to resolve by scanning related products
				for (Long pid : productIds) {
					try {
						Optional<PublicCustomerMenuJdbcRepository.ProductRow> prodOpt = menuJdbcRepository.findProductById(tenant.getId(), pid);
						if (prodOpt.isEmpty()) continue;
						var prod = prodOpt.get();
						String meta = prod.metadataJson();
						if (meta == null || meta.isBlank()) continue;
						try {
							var root = objectMapper.readTree(meta);
							long baseId = -Math.abs(prod.id());
							long variantsBase = baseId * 100L;
							long extrasBase = baseId * 1000L;
							if (root.has("variants") && root.get("variants").isArray()) {
								var arr = root.get("variants");
								for (int i = 0; i < arr.size(); i++) {
									long candidate = variantsBase - (i + 1);
									if (candidate == optId.longValue()) {
										int priceDelta = 0;
										if (arr.get(i).has("priceDelta")) {
											priceDelta = toCents(new BigDecimal(arr.get(i).path("priceDelta").asDouble(0.0)));
										}
										String name = arr.get(i).path("name").asText();
										PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow synthetic = new PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow(
											prod.id(),
											baseId - 1,
											"Varianti",
											optId.longValue(),
											name,
											BigDecimal.valueOf(priceDelta).movePointLeft(2)
										);
										modifierOptionsById = new java.util.HashMap<>(modifierOptionsById);
										modifierOptionsById.put(optId, synthetic);
										break;
									}
								}
							}
							if (root.has("extras") && root.get("extras").isArray()) {
								var arr = root.get("extras");
								for (int i = 0; i < arr.size(); i++) {
									long candidate = extrasBase - (i + 1);
									if (candidate == optId.longValue()) {
										int priceDelta = 0;
										if (arr.get(i).has("priceDelta")) {
											priceDelta = toCents(new BigDecimal(arr.get(i).path("priceDelta").asDouble(0.0)));
										}
										String name = arr.get(i).path("name").asText();
										PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow synthetic = new PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow(
											prod.id(),
											baseId - 2,
											"Extra",
											optId.longValue(),
											name,
											BigDecimal.valueOf(priceDelta).movePointLeft(2)
										);
										modifierOptionsById = new java.util.HashMap<>(modifierOptionsById);
										modifierOptionsById.put(optId, synthetic);
										break;
									}
								}
							}
						} catch (Exception e) {
							// ignore malformed metadata
						}
					} catch (Exception e) {
						// ignore DB read issues per-product
					}
				}
			}
		}

		List<LineComputation> computedLines = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (var requestLine : requestLines) {
			var product = productsById.get(requestLine.getTenantProductId());
			if (product == null) {
				throw new IllegalArgumentException("Prodotto non disponibile: " + requestLine.getTenantProductId());
			}

			List<Long> selectedOptionIds = requestLine.getSelectedModifierOptionIds() == null
				? List.of()
				: requestLine.getSelectedModifierOptionIds().stream().filter(Objects::nonNull).distinct().toList();

			List<PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow> selectedOptions = new ArrayList<>();
			int selectedDeltaCents = 0;
			List<String> optionNames = new ArrayList<>();
			for (Long optionId : selectedOptionIds) {
				var opt = modifierOptionsById.get(optionId);
				if (opt == null) {
					throw new IllegalArgumentException("Opzione non valida: " + optionId);
				}
				if (opt.tenantProductId() != product.id()) {
					throw new IllegalArgumentException("Opzione non associata al prodotto: " + optionId);
				}
				selectedOptions.add(opt);
				selectedDeltaCents += opt.priceDeltaCents();
				if (opt.optionName() != null && !opt.optionName().isBlank()) {
					optionNames.add(opt.optionName().trim());
				}
			}

			int basePriceCents = product.priceCents();
			int unitPriceCents = basePriceCents + selectedDeltaCents;
			BigDecimal unitPrice = new BigDecimal(unitPriceCents).movePointLeft(2);
			BigDecimal lineTotal = unitPrice.multiply(new BigDecimal(requestLine.getQuantity()));

			String nameSnapshot = product.name();
			if (!optionNames.isEmpty()) {
				nameSnapshot = appendOptionsSuffix(nameSnapshot, optionNames);
			}

			computedLines.add(new LineComputation(
				product.id(),
				nameSnapshot,
				product.department() == null ? "BAR" : product.department(),
				requestLine.getQuantity(),
				unitPrice,
				lineTotal,
				selectedOptions
			));

			totalAmount = totalAmount.add(lineTotal);
		}

		OrderEntity order = OrderEntity.builder()
			.tenantId(tenant.getId())
			.locationId(location.locationId())
			.locationLabelSnapshot(location.locationLabel())
			.areNameSnapshot(location.areaName())
			.source("QR")
			.status("NEW")
			.paymentStatus("NONE")
			.customerNote(request.getCustomerNote())
			.internalNote(null)
			.subtotalAmount(totalAmount)
			.totalAmount(totalAmount)
			.createdByStaffId(null)
			.acceptedByStaffId(null)
			.deliveredByStaffId(null)
			.acceptedAt(null)
			.readyAt(null)
			.deliveredAt(null)
			.createdAt(now)
			.updatedAt(now)
			.build();

		OrderEntity savedOrder = orderRepository.save(order);

		// Create order items
		List<OrderItemEntity> savedItems = new ArrayList<>();
		List<MapSqlParameterSource> modifierRows = new ArrayList<>();
		for (var line : computedLines) {
			OrderItemEntity orderItem = OrderItemEntity.builder()
				.orderId(savedOrder.getId())
				.tenantId(tenant.getId())
				.tenantProductId(line.tenantProductId)
				.productNameSnapshot(line.productNameSnapshot)
				.unitPriceSnapshot(line.unitPrice)
				.quantity(line.quantity)
				.lineTotal(line.lineTotal)
				.departmentSnapshot(line.department)
				.notes(null)
				.createdAt(now)
				.updatedAt(now)
				.build();

			OrderItemEntity savedItem = orderItemRepository.save(orderItem);
			savedItems.add(savedItem);

			if (line.selectedOptions != null) {
				for (var opt : line.selectedOptions) {
					modifierRows.add(new MapSqlParameterSource()
						.addValue("orderItemId", savedItem.getId())
						.addValue("optionId", opt.optionId())
						.addValue("groupName", opt.groupName())
						.addValue("optionName", opt.optionName())
						.addValue("priceDelta", opt.priceDelta() == null ? BigDecimal.ZERO : opt.priceDelta())
					);
				}
			}
		}
		batchInsertModifierOptionSnapshots(modifierRows);

		orderEventPublisher.publishOrderCreated(savedOrder.getId(), tenant.getId());

		// Build response
		return new CreatePublicOrderResponse(
			savedOrder.getId(),
			savedOrder.getStatus(),
			savedOrder.getTotalAmount(),
			savedItems.stream()
				.map(item -> new CreatePublicOrderResponse.OrderItemLine(
					item.getProductNameSnapshot(),
					item.getQuantity(),
					item.getUnitPriceSnapshot(),
					item.getLineTotal()
				))
				.collect(Collectors.toList())
		);
	}

	public Map<String, String> getPublicOrderStatus(long orderId) {
		OrderEntity order = orderRepository.findById(orderId)
			.orElseThrow(() -> new ResourceNotFoundException("Ordine non trovato"));

		return Map.of("status", order.getStatus());
	}

	private void batchInsertModifierOptionSnapshots(List<MapSqlParameterSource> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		// Filter out synthetic option ids (negative) because the DB table has a
		// foreign key to modifier_options.id and synthetic ids don't exist there.
		List<MapSqlParameterSource> filtered = rows.stream()
			.filter(r -> {
				Object v = r.getValue("optionId");
				if (v == null) return false;
				try {
					long val = ((Number) v).longValue();
					return val > 0;
				} catch (Exception ex) {
					return false;
				}
			})
			.toList();
		if (filtered.isEmpty()) return;
		String sql = """
			insert into order_item_modifier_options (
				order_item_id,
				modifier_option_id,
				modifier_group_name_snapshot,
				option_name_snapshot,
				price_delta_snapshot
			) values (
				:orderItemId,
				:optionId,
				:groupName,
				:optionName,
				:priceDelta
			)
		""";
		jdbc.batchUpdate(sql, filtered.toArray(MapSqlParameterSource[]::new));
	}

	private static String appendOptionsSuffix(String baseName, List<String> optionNames) {
		String safeBase = baseName == null ? "" : baseName.trim();
		String joined = String.join(", ", optionNames);
		String suffix = " (" + joined + ")";
		String result = safeBase + suffix;
		if (result.length() <= 200) {
			return result;
		}
		int max = 200;
		return result.substring(0, Math.max(0, max - 1)) + "…";
	}

	private static final class LineComputation {
		final long tenantProductId;
		final String productNameSnapshot;
		final String department;
		final int quantity;
		final BigDecimal unitPrice;
		final BigDecimal lineTotal;
		final List<PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow> selectedOptions;

		LineComputation(
			long tenantProductId,
			String productNameSnapshot,
			String department,
			int quantity,
			BigDecimal unitPrice,
			BigDecimal lineTotal,
			List<PublicCustomerMenuJdbcRepository.ModifierOptionForOrderRow> selectedOptions
		) {
			this.tenantProductId = tenantProductId;
			this.productNameSnapshot = productNameSnapshot;
			this.department = department;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
			this.lineTotal = lineTotal;
			this.selectedOptions = selectedOptions;
		}
	}

	private boolean isOrderingEnabledByTenantSettings(Tenant tenant) {
		JsonNode root = readOpeningConfigOrEmpty(tenant.getOpeningConfigJson());

		boolean orderingPaused = root.path("orderingPaused").asBoolean(false);
		if (orderingPaused) {
			return false;
		}

		JsonNode opening = root.path("openingHours");
		String open = opening.path("open").asText("00:00");
		String close = opening.path("close").asText("23:59");

		Integer openMinutes = parseMinutes(open);
		Integer closeMinutes = parseMinutes(close);
		if (openMinutes == null || closeMinutes == null) {
			return true;
		}

		ZoneId zone;
		try {
			zone = tenant.getTimezone() != null ? ZoneId.of(tenant.getTimezone()) : ZoneId.systemDefault();
		} catch (Exception ex) {
			zone = ZoneId.systemDefault();
		}

		LocalTime now = ZonedDateTime.now(zone).toLocalTime();
		int nowMinutes = now.getHour() * 60 + now.getMinute();

		boolean wraps = closeMinutes < openMinutes;
		if (!wraps) {
			return nowMinutes >= openMinutes && nowMinutes <= closeMinutes;
		}

		return nowMinutes >= openMinutes || nowMinutes <= closeMinutes;
	}

	private JsonNode readOpeningConfigOrEmpty(String json) {
		if (json == null || json.isBlank()) {
			return objectMapper.createObjectNode();
		}
		try {
			JsonNode parsed = objectMapper.readTree(json);
			return parsed != null ? parsed : objectMapper.createObjectNode();
		} catch (Exception ex) {
			return objectMapper.createObjectNode();
		}
	}

	private Integer parseMinutes(String time) {
		if (time == null) {
			return null;
		}

		String trimmed = time.trim();
		if (trimmed.length() != 5 || trimmed.charAt(2) != ':') {
			return null;
		}

		try {
			int hh = Integer.parseInt(trimmed.substring(0, 2));
			int mm = Integer.parseInt(trimmed.substring(3, 5));
			if (hh < 0 || hh > 23 || mm < 0 || mm > 59) {
				return null;
			}
			return hh * 60 + mm;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static int toCents(BigDecimal price) {
		if (price == null) return 0;
		return price.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValue();
	}
}

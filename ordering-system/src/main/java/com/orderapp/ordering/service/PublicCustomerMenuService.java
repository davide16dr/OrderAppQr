package com.orderapp.ordering.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderapp.ordering.dto.CustomerMenuViewModelDTO;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.PublicCustomerMenuJdbcRepository;
import com.orderapp.ordering.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicCustomerMenuService {
	private final TenantRepository tenantRepository;
	private final PublicCustomerMenuJdbcRepository menuJdbcRepository;
	private final ObjectMapper objectMapper;

	public CustomerMenuViewModelDTO getMenu(String token, String tenant, String location) {
		Resolved resolved = resolveTenantAndLocation(token, tenant, location);
		boolean orderingEnabled = isOrderingEnabledByTenantSettings(resolved.tenant());
		boolean menuActive = resolved.active() && orderingEnabled;

		List<CustomerMenuViewModelDTO.MenuCategoryDTO> categories = menuJdbcRepository
			.findActiveCategories(resolved.tenant().getId())
			.stream()
			.map(c -> new CustomerMenuViewModelDTO.MenuCategoryDTO(String.valueOf(c.id()), c.name(), null))
			.toList();

		List<PublicCustomerMenuJdbcRepository.ProductRow> productRows =
			menuJdbcRepository.findActiveProductsWithPrimaryCategory(resolved.tenant().getId());

		List<Long> productIds = productRows.stream().map(PublicCustomerMenuJdbcRepository.ProductRow::id).toList();
		Map<Long, List<CustomerMenuViewModelDTO.ModifierGroupDTO>> modifiersByProduct =
			buildModifiersMap(resolved.tenant().getId(), productIds);

		List<CustomerMenuViewModelDTO.MenuProductDTO> products = productRows.stream()
			.map(p -> {
				var groups = modifiersByProduct.getOrDefault(p.id(), List.of());
				if ((groups == null || groups.isEmpty()) && p.metadataJson() != null && !p.metadataJson().isBlank()) {
					try {
						var meta = objectMapper.readTree(p.metadataJson());
						List<CustomerMenuViewModelDTO.ModifierGroupDTO> generated = new ArrayList<>();
						long baseId = -Math.abs(p.id());

						if (meta.has("variants") && meta.get("variants").isArray()) {
							var arr = meta.get("variants");
							List<CustomerMenuViewModelDTO.ModifierOptionDTO> opts = new ArrayList<>();
							for (int i = 0; i < arr.size(); i++) {
								var it = arr.get(i);
								long optId = baseId * 100L - (i + 1);
								Integer priceCents = null;
								Integer priceDelta = null;
								if (it.has("price")) {
									priceCents = toCents(BigDecimal.valueOf(it.path("price").asDouble(0.0)));
								} else if (it.has("priceDelta")) {
									priceDelta = toCents(BigDecimal.valueOf(it.path("priceDelta").asDouble(0.0)));
								}
								opts.add(new CustomerMenuViewModelDTO.ModifierOptionDTO(optId, it.path("name").asText(), priceCents, priceDelta));
							}
							generated.add(new CustomerMenuViewModelDTO.ModifierGroupDTO(baseId - 1, "Varianti", 0, 1, false, opts));
						}

						if (meta.has("extras") && meta.get("extras").isArray()) {
							var arr = meta.get("extras");
							List<CustomerMenuViewModelDTO.ModifierOptionDTO> opts = new ArrayList<>();
							for (int i = 0; i < arr.size(); i++) {
								var it = arr.get(i);
								long optId = baseId * 1000L - (i + 1);
								Integer priceDelta = null;
								if (it.has("priceDelta")) {
									priceDelta = toCents(BigDecimal.valueOf(it.path("priceDelta").asDouble(0.0)));
								}
								opts.add(new CustomerMenuViewModelDTO.ModifierOptionDTO(optId, it.path("name").asText(), null, priceDelta));
							}
							generated.add(new CustomerMenuViewModelDTO.ModifierGroupDTO(baseId - 2, "Extra", 0, Math.max(1, arr.size()), false, opts));
						}

						if (!generated.isEmpty()) {
							groups = generated;
						}
					} catch (Exception ex) {
						// Ignore metadata parse errors and keep groups empty
					}
				}
				return new CustomerMenuViewModelDTO.MenuProductDTO(
					String.valueOf(p.id()),
					p.name(),
					p.description(),
					toCents(p.price()),
					p.imageUrl(),
					null,
					String.valueOf(p.categoryId()),
					groups
				);
			})
			.toList();

		CustomerMenuViewModelDTO.LocationContextDTO context = new CustomerMenuViewModelDTO.LocationContextDTO(
			resolved.tenantName(),
			avatarText(resolved.tenantName()),
			readBrandingLogoDataUrl(resolved.tenant()),
			resolved.locationLabel(),
			resolved.areaName(),
			menuActive ? "Attivo" : "Non attivo",
			menuActive ? "active" : "inactive"
		);

		return new CustomerMenuViewModelDTO(context, categories, products);
	}

	private Map<Long, List<CustomerMenuViewModelDTO.ModifierGroupDTO>> buildModifiersMap(long tenantId, List<Long> productIds) {
		List<PublicCustomerMenuJdbcRepository.ModifierGroupRow> rows =
			menuJdbcRepository.findModifierGroupsForProducts(tenantId, productIds);

		// product → group → options (preserve insertion order)
		Map<Long, Map<Long, GroupAccumulator>> acc = new LinkedHashMap<>();
		for (var row : rows) {
			acc.computeIfAbsent(row.productId(), k -> new LinkedHashMap<>())
				.computeIfAbsent(row.groupId(), k -> new GroupAccumulator(
					row.groupId(), row.groupName(), row.minSelectable(), row.maxSelectable(), row.required()
				))
				.options.add(new CustomerMenuViewModelDTO.ModifierOptionDTO(
					row.optionId(), row.optionName(), row.priceCents(), row.priceDeltaCents()
				));
		}

		Map<Long, List<CustomerMenuViewModelDTO.ModifierGroupDTO>> result = new LinkedHashMap<>();
		acc.forEach((productId, groups) -> {
			List<CustomerMenuViewModelDTO.ModifierGroupDTO> dtos = new ArrayList<>();
			groups.forEach((groupId, g) -> dtos.add(new CustomerMenuViewModelDTO.ModifierGroupDTO(
				g.id, g.name, g.minSelectable, g.maxSelectable, g.required, g.options
			)));
			result.put(productId, dtos);
		});
		return result;
	}

	private record GroupAccumulator(
		long id, String name, int minSelectable, Integer maxSelectable, boolean required,
		List<CustomerMenuViewModelDTO.ModifierOptionDTO> options
	) {
		GroupAccumulator(long id, String name, int minSelectable, Integer maxSelectable, boolean required) {
			this(id, name, minSelectable, maxSelectable, required, new ArrayList<>());
		}
	}

	private Resolved resolveTenantAndLocation(String token, String tenant, String location) {
		if (token != null && !token.isBlank()) {
			Optional<PublicCustomerMenuJdbcRepository.ResolvedContextRow> byToken = menuJdbcRepository
				.findResolvedContextByToken(token.trim());
			PublicCustomerMenuJdbcRepository.ResolvedContextRow ctx = byToken.orElseThrow(() ->
				new IllegalArgumentException("Token location non valido o scaduto")
			);

			boolean active = "ACTIVE".equalsIgnoreCase(ctx.locationStatus());
			Tenant resolvedTenant = tenantRepository.findById(ctx.tenantId())
				.orElseThrow(() -> new IllegalArgumentException("Tenant non trovato o non attivo"));

			return new Resolved(resolvedTenant, ctx.tenantName(), ctx.locationLabel(), ctx.areaName(), active);
		}

		Tenant resolvedTenant;
		if (tenant == null || tenant.isBlank()) {
			List<Tenant> activeTenants = tenantRepository.findAll().stream()
				.filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
				.toList();
			if (activeTenants.size() != 1) {
				throw new IllegalArgumentException("Parametro 'tenant' mancante");
			}
			resolvedTenant = activeTenants.get(0);
		} else {
			resolvedTenant = tenantRepository
				.findBySubdomainIgnoreCase(tenant.trim())
				.or(() -> tenantRepository.findBySlugIgnoreCase(tenant.trim()))
				.filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
				.orElseThrow(() -> new IllegalArgumentException("Tenant non trovato o non attivo"));
		}

		PublicCustomerMenuJdbcRepository.LocationRow loc;
		if (location != null && !location.isBlank()) {
			loc = menuJdbcRepository
				.findLocationByLabel(resolvedTenant.getId(), location.trim())
				.orElseThrow(() -> new IllegalArgumentException("Location non trovata"));
		} else {
			loc = menuJdbcRepository
				.findFirstActiveLocation(resolvedTenant.getId())
				.orElseThrow(() -> new IllegalArgumentException("Nessuna location attiva trovata"));
		}

		boolean active = "ACTIVE".equalsIgnoreCase(loc.locationStatus());
		return new Resolved(resolvedTenant, resolvedTenant.getName(), loc.locationLabel(), loc.areaName(), active);
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

	private String readBrandingLogoDataUrl(Tenant tenant) {
		String brandingJson = tenant.getBrandingJson();
		if (brandingJson == null || brandingJson.isBlank()) {
			return null;
		}

		try {
			JsonNode root = objectMapper.readTree(brandingJson);
			JsonNode logoNode = root.path("logoDataUrl");
			if (logoNode.isTextual() && !logoNode.asText().isBlank()) {
				return logoNode.asText();
			}
		} catch (Exception ex) {
			// ignore malformed branding json and fall back to initials
		}

		return null;
	}

	private static String avatarText(String businessName) {
		if (businessName == null) {
			return null;
		}
		String trimmed = businessName.trim();
		return trimmed.isEmpty() ? null : trimmed.substring(0, 1).toUpperCase();
	}

	private static int toCents(BigDecimal price) {
		if (price == null) {
			return 0;
		}
		BigDecimal normalized = price.setScale(2, RoundingMode.HALF_UP);
		try {
			return normalized.movePointRight(2).intValueExact();
		} catch (ArithmeticException ex) {
			throw new IllegalArgumentException("Prezzo non valido: " + price);
		}
	}

	private record Resolved(Tenant tenant, String tenantName, String locationLabel, String areaName, boolean active) {}
}

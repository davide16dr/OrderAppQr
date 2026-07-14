package com.orderapp.ordering.service;

import com.orderapp.ordering.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoCleanupScheduler {

	private final TenantRepository tenantRepository;
	private final NamedParameterJdbcTemplate jdbc;

	@Scheduled(fixedDelay = 1_800_000) // ogni 30 minuti
	@Transactional
	public void deleteStaleOrders() {
		var demoTenants = tenantRepository.findAll()
			.stream()
			.filter(com.orderapp.ordering.entity.Tenant::isDemo)
			.toList();

		if (demoTenants.isEmpty()) return;

		OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(30);

		for (var tenant : demoTenants) {
			MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("tenantId", tenant.getId())
				.addValue("cutoff", cutoff);

			int items = jdbc.update(
				"DELETE FROM order_items WHERE order_id IN " +
				"(SELECT id FROM orders WHERE tenant_id = :tenantId AND created_at < :cutoff)",
				params);

			int orders = jdbc.update(
				"DELETE FROM orders WHERE tenant_id = :tenantId AND created_at < :cutoff",
				params);

			if (orders > 0) {
				log.info("Demo cleanup: eliminati {} ordini ({} righe) per tenant {}",
					orders, items, tenant.getId());
			}
		}
	}
}

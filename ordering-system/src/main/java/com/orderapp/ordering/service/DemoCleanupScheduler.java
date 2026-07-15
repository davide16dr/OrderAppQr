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

	@Scheduled(fixedDelay = 1_800_000, initialDelay = 0) // subito all'avvio, poi ogni 30 min
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

			// Il trigger fn_prevent_edit_closed_orders blocca DELETE su ordini DELIVERED;
			// lo disabilito per la durata della pulizia demo.
			jdbc.getJdbcTemplate().execute("ALTER TABLE order_items DISABLE TRIGGER ALL");
			jdbc.getJdbcTemplate().execute("ALTER TABLE orders DISABLE TRIGGER ALL");

			try {
				jdbc.update(
					"DELETE FROM order_items WHERE order_id IN " +
					"(SELECT id FROM orders WHERE tenant_id = :tenantId AND created_at < :cutoff)",
					params);

				jdbc.update(
					"DELETE FROM order_status_history WHERE order_id IN " +
					"(SELECT id FROM orders WHERE tenant_id = :tenantId AND created_at < :cutoff)",
					params);

				int orders = jdbc.update(
					"DELETE FROM orders WHERE tenant_id = :tenantId AND created_at < :cutoff",
					params);

				if (orders > 0) {
					log.info("Demo cleanup: eliminati {} ordini per tenant {}", orders, tenant.getId());
				}
			} finally {
				jdbc.getJdbcTemplate().execute("ALTER TABLE order_items ENABLE TRIGGER ALL");
				jdbc.getJdbcTemplate().execute("ALTER TABLE orders ENABLE TRIGGER ALL");
			}
		}
	}
}

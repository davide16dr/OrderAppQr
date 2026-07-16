package com.orderapp.ordering.service;

import com.orderapp.ordering.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoCleanupScheduler {

	private final TenantRepository tenantRepository;
	private final NamedParameterJdbcTemplate jdbc;

	/** Ogni 30 min elimina gli ordini demo più vecchi di 30 minuti. */
	@Scheduled(fixedDelay = 1_800_000, initialDelay = 0)
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

	/**
	 * Ogni giorno alle 03:00 Europe/Rome: per ogni tenant non-demo, se oggi
	 * è un multiplo esatto di 15 giorni dalla data di creazione account,
	 * elimina tutti i suoi ordini per tenere il DB leggero.
	 */
	@Scheduled(cron = "0 0 3 * * *", zone = "Europe/Rome")
	@Transactional
	public void deleteTenantOrdersPeriodically() {
		ZoneId rome = ZoneId.of("Europe/Rome");
		LocalDate today = LocalDate.now(rome);

		var tenants = tenantRepository.findAll()
			.stream()
			.filter(t -> !t.isDemo())
			.toList();

		for (var tenant : tenants) {
			if (tenant.getCreatedAt() == null) continue;

			LocalDate createdDate = tenant.getCreatedAt().atZoneSameInstant(rome).toLocalDate();
			long daysSince = ChronoUnit.DAYS.between(createdDate, today);

			if (daysSince <= 0 || daysSince % 15 != 0) continue;

			MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("tenantId", tenant.getId());

			jdbc.getJdbcTemplate().execute("ALTER TABLE order_items DISABLE TRIGGER ALL");
			jdbc.getJdbcTemplate().execute("ALTER TABLE orders DISABLE TRIGGER ALL");

			try {
				jdbc.update("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE tenant_id = :tenantId)", params);
				jdbc.update("DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE tenant_id = :tenantId)", params);
				int deleted = jdbc.update("DELETE FROM orders WHERE tenant_id = :tenantId", params);

				if (deleted > 0) {
					log.info("Pulizia periodica: eliminati {} ordini per tenant {} ({}° ciclo 15gg)",
						deleted, tenant.getId(), daysSince / 15);
				}
			} finally {
				jdbc.getJdbcTemplate().execute("ALTER TABLE order_items ENABLE TRIGGER ALL");
				jdbc.getJdbcTemplate().execute("ALTER TABLE orders ENABLE TRIGGER ALL");
			}
		}
	}
}

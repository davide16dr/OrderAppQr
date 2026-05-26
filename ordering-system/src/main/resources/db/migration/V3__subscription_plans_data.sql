-- =========================================================
-- 3) SUBSCRIPTION PLANS INITIAL DATA
-- =========================================================

INSERT INTO subscription_plans (code, name, description, price_monthly, price_yearly, max_locations, max_staff_users, max_products, qr_batch_enabled, realtime_dashboard, global_catalog_enabled, is_active, created_at, updated_at) VALUES
('BASIC', 'Piano Base', 'Perfetto per iniziare', 29.99, 299.99, 1, 2, 50, false, true, true, true, now(), now()),
('PROFESSIONAL', 'Piano Professionale', 'Per attività in crescita', 79.99, 799.99, 5, 10, 500, true, true, true, true, now(), now()),
('ENTERPRISE', 'Piano Enterprise', 'Per grandi aziende', 199.99, 1999.99, null, null, null, true, true, true, true, now(), now());

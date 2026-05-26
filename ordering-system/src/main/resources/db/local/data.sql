insert into tenants (
  id, slug, subdomain, name, legal_name, business_type, status, enabled, timezone, currency_code,
  country, registration_source, created_at, updated_at, branding_json, opening_config_json
) values 
  (1, 'lido-azzurro', 'lido-azzurro', 'Lido Azzurro', null, 'LIDO', 'ACTIVE', true, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}'),
  (2, 'beachbar-sunset', 'beachbar-sunset', 'Beach Bar Sunset', null, 'BAR', 'ACTIVE', true, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}'),
  (3, 'pizzeria-napoli', 'pizzeria-napoli', 'Pizzeria Napoli', null, 'RESTAURANT', 'ACTIVE', true, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}'),
  (4, 'gelato-artigianale', 'gelato-artigianale', 'Gelato Artigianale', null, 'OTHER', 'ACTIVE', true, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}'),
  (5, 'hotel-imperial', 'hotel-imperial', 'Hotel Imperial', null, 'OTHER', 'PENDING', false, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}'),
  (6, 'coffee-lab', 'coffee-lab', 'Coffee Lab', null, 'OTHER', 'ACTIVE', true, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}'),
  (7, 'disco-fuego', 'disco-fuego', 'Disco Fuego', null, 'NIGHTCLUB', 'ACTIVE', false, 'Europe/Rome', 'EUR',
   'Italy', 'SELF_SIGNUP', current_timestamp, current_timestamp, '{}', '{}');

insert into areas (id, tenant_id, name, display_order, status, created_at, updated_at)
values (10, 1, 'Zona Spiaggia', 0, 'ACTIVE', current_timestamp, current_timestamp);

insert into locations (id, tenant_id, area_id, type, label, status, operational_status, capacity, metadata_json, created_at, updated_at)
values (100, 1, 10, 'UMBRELLA', 'Ombrellone 42', 'ACTIVE', 'AVAILABLE', null, '{}', current_timestamp, current_timestamp);

insert into location_tokens (
  id, tenant_id, location_id, token, status, is_primary, rotatable, expires_at, qr_value, created_at, updated_at
) values (
  1000, 1, 100, 'LOCAL_TOKEN_UMBRELLONE_42_000000', 'ACTIVE', true, true, null,
  'http://localhost:4202/customer/menu?tenant=lido-azzurro&token=LOCAL_TOKEN_UMBRELLONE_42_000000',
  current_timestamp, current_timestamp
);

insert into categories (id, tenant_id, name, description, display_order, status, created_at, updated_at)
values
  (10000, 1, 'Bevande', null, 0, 'ACTIVE', current_timestamp, current_timestamp),
  (10001, 1, 'Cibo', null, 1, 'ACTIVE', current_timestamp, current_timestamp),
  (10002, 1, 'Dolci', null, 2, 'ACTIVE', current_timestamp, current_timestamp);

insert into tenant_products (
  id, tenant_id, global_product_id, sku, name, description, price, image_url,
  department, vat_rate, status, available_for_order, is_customized, metadata_json, created_at, updated_at
) values
  (20000, 1, null, 'COCA-33', 'Coca Cola', 'Lattina 33cl', 3.50, null, 'BAR', 10.00, 'ACTIVE', true, false, '{}', current_timestamp, current_timestamp),
  (20001, 1, null, 'ACQUA-50', 'Acqua Naturale', 'Bottiglia 50cl', 2.00, null, 'BAR', 10.00, 'ACTIVE', true, false, '{}', current_timestamp, current_timestamp),
  (20002, 1, null, 'PIZZA-MAR', 'Pizza Margherita', 'Pomodoro, mozzarella, basilico', 8.00, null, 'KITCHEN', 10.00, 'ACTIVE', true, false, '{}', current_timestamp, current_timestamp),
  (20003, 1, null, 'TIRAMISU', 'Tiramisù', 'Porzione', 4.50, null, 'KITCHEN', 10.00, 'ACTIVE', true, false, '{}', current_timestamp, current_timestamp);

insert into category_tenant_products (category_id, tenant_product_id, display_order)
values
  (10000, 20000, 0),
  (10000, 20001, 1),
  (10001, 20002, 0),
  (10002, 20003, 0);

insert into staff_roles (code, description)
values ('MANAGER', 'Manager');

insert into subscription_plans (
  code, name, description, price_monthly, price_yearly, max_locations, max_staff_users, max_products,
  qr_batch_enabled, realtime_dashboard, global_catalog_enabled, is_active, created_at, updated_at
) values
  ('BASIC', 'Piano Base', 'Perfetto per iniziare', 29.99, 299.99, 1, 2, 50, false, true, true, true, current_timestamp, current_timestamp),
  ('PROFESSIONAL', 'Piano Professionale', 'Per attività in crescita', 79.99, 799.99, 5, 10, 500, true, true, true, true, current_timestamp, current_timestamp),
  ('ENTERPRISE', 'Piano Enterprise', 'Per grandi aziende', 199.99, 1999.99, null, null, null, true, true, true, true, current_timestamp, current_timestamp);

-- Seed minimo per dashboard staff (fatturato/ordini) in profilo local
insert into orders (
  id, tenant_id, location_id, location_label_snapshot, area_name_snapshot,
  status, customer_note, total_amount, created_at, delivered_at, delivered_by_staff_id, updated_at
) values
  (
    50000, 1, 100, 'Ombrellone 42', 'Zona Spiaggia',
    'DELIVERED', 'Senza ghiaccio', 11.50,
    DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP), null, CURRENT_TIMESTAMP
  ),
  (
    50001, 1, 100, 'Ombrellone 42', 'Zona Spiaggia',
    'NEW', null, 8.00,
    DATEADD('MINUTE', -25, CURRENT_TIMESTAMP), null, null, CURRENT_TIMESTAMP
  ),
  (
    50002, 1, 100, 'Ombrellone 42', 'Zona Spiaggia',
    'DELIVERED', null, 4.50,
    DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -3, DATEADD('MINUTE', 18, CURRENT_TIMESTAMP)), null, CURRENT_TIMESTAMP
  );

insert into order_items (
  id, tenant_id, order_id, tenant_product_id, quantity,
  product_name_snapshot, unit_price_snapshot, department_snapshot, line_total, created_at
) values
  (60000, 1, 50000, 20000, 1, 'Coca Cola', 3.50, 'BAR', 3.50, DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
  (60001, 1, 50000, 20002, 1, 'Pizza Margherita', 8.00, 'KITCHEN', 8.00, DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
  (60002, 1, 50001, 20002, 1, 'Pizza Margherita', 8.00, 'KITCHEN', 8.00, DATEADD('MINUTE', -25, CURRENT_TIMESTAMP)),
  (60003, 1, 50002, 20003, 1, 'Tiramisù', 4.50, 'KITCHEN', 4.50, DATEADD('DAY', -3, CURRENT_TIMESTAMP));

-- ===== STAFF ROLES - COMPLETE ROLE HIERARCHY =====
INSERT INTO staff_roles (code, description) VALUES
  ('SUPER_ADMIN', 'Sistema Administrator - Full access to all tenants and system settings'),
  ('ADMIN', 'Tenant Administrator - Full access to their tenant'),
  ('STAFF', 'Tenant Staff - Can manage orders and view assigned areas'),
  ('VIEWER', 'Read-only access - Can view dashboards and reports only')
ON CONFLICT (code) DO NOTHING;

-- ===== SUPERADMIN USER (GLOBAL SYSTEM ADMINISTRATOR) =====
-- Default credentials for initial setup:
-- Email: admin@orderapp.local
-- Password: SecureAdmin@2024!
-- Password Hash (bcrypt): $2a$10$1K2jK9nZ7pM4vL8qR5sT6u.YqJ3hGf9xB2cD5eF8pK1lM4nO7rQ0
INSERT INTO staff_users (
  id, tenant_id, first_name, last_name, email, password_hash, phone,
  is_primary_contact, activated_at, status, created_at, updated_at
) VALUES (
  1, NULL, 'System', 'Administrator', 'admin@orderapp.local',
  '$2a$10$1K2jK9nZ7pM4vL8qR5sT6u.YqJ3hGf9xB2cD5eF8pK1lM4nO7rQ0',
  NULL, true, CURRENT_TIMESTAMP, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Assign SUPER_ADMIN role to superadmin user
INSERT INTO staff_user_roles (staff_user_id, role_id)
SELECT 1, id FROM staff_roles WHERE code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- ===== STAFF USERS FOR DEMO TENANTS =====
INSERT INTO staff_users (
  tenant_id, first_name, last_name, email, password_hash, phone,
  is_primary_contact, activated_at, status, created_at, updated_at
) VALUES
  (1, 'Marco', 'Rossi', 'marco@lido-azzurro.local',
   '$2a$10$9zK3jL7mN4bQ8pR1sT2uV3.WqJ0hGf6xB1cD4eF7pK0lM3nO6rQ9',
   '+39 06 1111111', true, CURRENT_TIMESTAMP, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'Luigi', 'Bianchi', 'luigi@beach-bar-sunset.local',
   '$2a$10$2K9mL5bN7pQ3sT6uV0.WxJ1hGf4xB8cD2eF5pK9lM6nO0rQ3sT4',
   '+39 06 2222222', true, CURRENT_TIMESTAMP, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'Giulia', 'Verdi', 'giulia@pizzeria-napoli.local',
   '$2a$10$3L0nM6cO8qR4tU7vW1.XyK2hGf5xB9cD3eF6pK0lM7nO1rQ4tU5',
   '+39 06 3333333', true, CURRENT_TIMESTAMP, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Assign ADMIN role to demo tenant staff users
INSERT INTO staff_user_roles (staff_user_id, role_id)
SELECT su.id, sr.id FROM staff_users su, staff_roles sr
WHERE su.tenant_id IS NOT NULL AND sr.code = 'ADMIN'
AND su.id NOT IN (SELECT staff_user_id FROM staff_user_roles WHERE role_id IN (SELECT id FROM staff_roles WHERE code = 'ADMIN'))
ON CONFLICT DO NOTHING;

-- ===== SUBSCRIPTION PLANS - UPDATE COMPLETE OFFERINGS =====
DELETE FROM subscription_plans WHERE code IN ('BASIC', 'PROFESSIONAL', 'ENTERPRISE');
INSERT INTO subscription_plans (
  code, name, description, price_monthly, price_yearly, max_locations, max_staff_users, max_products,
  qr_batch_enabled, realtime_dashboard, global_catalog_enabled, is_active, created_at, updated_at
) VALUES
  ('BASIC', 'Piano Base', 'Perfetto per piccole attività single-location', 9.99, 99.90, 1, 3, 100,
   false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('PROFESSIONAL', 'Piano Professionale', 'Ideale per ristoranti e bar multi-location', 49.99, 499.90, 5, 15, 500,
   true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ENTERPRISE', 'Piano Enterprise', 'Soluzione enterprise con supporto dedicato', 199.99, 1999.90, 9999, 9999, 9999,
   true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ===== TENANT SUBSCRIPTIONS - ASSIGN PLANS TO DEMO TENANTS =====
INSERT INTO tenant_subscriptions (
  tenant_id, plan_code, status, billing_cycle, payment_status, activated_at, created_at, updated_at
) VALUES
  (1, 'PROFESSIONAL', 'ACTIVE', 'MONTHLY', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'BASIC', 'ACTIVE', 'MONTHLY', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'PROFESSIONAL', 'ACTIVE', 'MONTHLY', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'BASIC', 'TRIAL', 'MONTHLY', 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 'BASIC', 'ACTIVE', 'MONTHLY', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id) DO NOTHING;

-- ===== VERIFICATION QUERIES =====
-- Uncomment to verify data was inserted correctly:
-- SELECT 'Staff roles' AS entity, COUNT(*) AS count FROM staff_roles UNION ALL
-- SELECT 'Subscription plans', COUNT(*) FROM subscription_plans UNION ALL
-- SELECT 'Tenants with subscriptions', COUNT(*) FROM tenant_subscriptions UNION ALL
-- SELECT 'Admin staff users', COUNT(*) FROM staff_users su JOIN staff_user_roles sur ON su.id = sur.staff_user_id WHERE sur.role_id = (SELECT id FROM staff_roles WHERE code = 'ADMIN');

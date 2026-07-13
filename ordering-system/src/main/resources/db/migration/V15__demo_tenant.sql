-- ── Demo tenant setup ──────────────────────────────────────────────────────────

-- 1. Add is_demo flag to tenants
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS is_demo BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Demo tenant
INSERT INTO tenants (slug, subdomain, name, legal_name, business_type, status, timezone,
                     currency_code, country, registration_source, enabled, is_demo,
                     branding_json, opening_config_json, created_at, updated_at)
VALUES ('demo', 'demo', 'Lido Azzurro', 'Lido Azzurro Demo S.r.l.', 'LIDO', 'ACTIVE',
        'Europe/Rome', 'EUR', 'Italy', 'SELF_SIGNUP', true, true,
        '{}', '{}', now(), now());

-- 3. Demo staff user  (password = Demo2024!)
INSERT INTO staff_users (tenant_id, first_name, last_name, email, password_hash,
                         is_primary_contact, activated_at, status, created_at, updated_at)
VALUES (
    (SELECT id FROM tenants WHERE slug = 'demo'),
    'Demo', 'User', 'demo@orderappqr.it',
    '$2b$10$d7SX9cDjoV5eH5FbgleJ3OOaU0Vv0u7JK228I5M/XUn2vUN1lFTjC',
    true, now(), 'ACTIVE', now(), now()
);

-- 4. Assign MANAGER role to demo user
INSERT INTO staff_user_roles (staff_user_id, role_id)
VALUES (
    (SELECT id FROM staff_users WHERE email = 'demo@orderappqr.it'),
    (SELECT id FROM staff_roles WHERE code = 'MANAGER')
);

-- 5. Subscription (ACTIVE, no Stripe)
INSERT INTO tenant_subscriptions (tenant_id, plan_code, status, billing_cycle,
                                   payment_status, current_period_start, current_period_end,
                                   activated_at, created_at, updated_at)
VALUES (
    (SELECT id FROM tenants WHERE slug = 'demo'),
    'BASIC', 'ACTIVE', 'MONTHLY', 'PAID',
    now(), now() + interval '30 days', now(), now(), now()
);

-- 6. Categories
INSERT INTO categories (tenant_id, name, display_order, status, created_at, updated_at) VALUES
    ((SELECT id FROM tenants WHERE slug = 'demo'), 'Cocktail', 1, 'ACTIVE', now(), now()),
    ((SELECT id FROM tenants WHERE slug = 'demo'), 'Bibite',   2, 'ACTIVE', now(), now()),
    ((SELECT id FROM tenants WHERE slug = 'demo'), 'Snack',    3, 'ACTIVE', now(), now());

-- 7. Products
INSERT INTO tenant_products (tenant_id, sku, name, description, price, department, vat_rate,
                              status, available_for_order, metadata_json, created_at, updated_at) VALUES
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-C01', 'Aperol Spritz',  'Prosecco, Aperol e soda. Fresco e vivace.', 7.00,  'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-C02', 'Mojito',         'Rum bianco, lime, menta e zucchero di canna.', 8.00, 'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-C03', 'Gin Tonic',      'Gin premium e acqua tonica con agrumi.', 8.50,  'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-C04', 'Negroni',        'Gin, vermouth rosso e bitter Campari.', 9.00,  'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-C05', 'Cosmopolitan',   'Vodka, Cointreau, lime e succo di mirtillo.', 8.00, 'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-B01', 'Acqua naturale', '50 cl.',                                   2.50,  'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-B02', 'Coca Cola',      'Lattina 33 cl.',                           3.00,  'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-B03', 'Succo di frutta','Pesca, albicocca o ananas — 20 cl.',        3.50,  'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-S01', 'Patatine',       'Chips croccanti in busta.',                4.00, 'BAR', 10, 'ACTIVE', true, '{}', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'DEMO-S02', 'Toast',          'Prosciutto cotto e formaggio, tostato.',   5.50, 'BAR', 10, 'ACTIVE', true, '{}', now(), now());

-- 8. Link products to categories
INSERT INTO category_tenant_products (category_id, tenant_product_id, display_order)
SELECT c.id, p.id, ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY p.id)
FROM categories c
JOIN tenant_products p ON p.tenant_id = c.tenant_id
WHERE c.tenant_id = (SELECT id FROM tenants WHERE slug = 'demo')
  AND (
    (c.name = 'Cocktail' AND p.sku IN ('DEMO-C01','DEMO-C02','DEMO-C03','DEMO-C04','DEMO-C05'))
    OR (c.name = 'Bibite'   AND p.sku IN ('DEMO-B01','DEMO-B02','DEMO-B03'))
    OR (c.name = 'Snack'    AND p.sku IN ('DEMO-S01','DEMO-S02'))
  );

-- 9. Area
INSERT INTO areas (tenant_id, name, display_order, status, description, created_at, updated_at) VALUES
    ((SELECT id FROM tenants WHERE slug='demo'), 'Spiaggia', 1, 'ACTIVE', 'Zona ombrelloni', now(), now()),
    ((SELECT id FROM tenants WHERE slug='demo'), 'Bar',      2, 'ACTIVE', 'Zona bar interno', now(), now());

-- 10. Locations (postazioni)
INSERT INTO locations (tenant_id, area_id, type, label, status, metadata_json, created_at, updated_at)
SELECT t.id, a.id, 'UMBRELLA', 'Ombrellone ' || n, 'ACTIVE', '{}', now(), now()
FROM tenants t
JOIN areas a ON a.tenant_id = t.id AND a.name = 'Spiaggia'
CROSS JOIN generate_series(1, 5) AS n
WHERE t.slug = 'demo';

INSERT INTO locations (tenant_id, area_id, type, label, status, metadata_json, created_at, updated_at)
SELECT t.id, a.id, 'TABLE', 'Tavolo ' || n, 'ACTIVE', '{}', now(), now()
FROM tenants t
JOIN areas a ON a.tenant_id = t.id AND a.name = 'Bar'
CROSS JOIN generate_series(1, 3) AS n
WHERE t.slug = 'demo';

-- 11. Sample orders (delivered + active)
WITH demo AS (SELECT id AS tid FROM tenants WHERE slug = 'demo'),
     loc1  AS (SELECT l.id FROM locations l JOIN demo d ON l.tenant_id = d.tid WHERE l.label = 'Ombrellone 1'),
     loc2  AS (SELECT l.id FROM locations l JOIN demo d ON l.tenant_id = d.tid WHERE l.label = 'Ombrellone 3'),
     loc3  AS (SELECT l.id FROM locations l JOIN demo d ON l.tenant_id = d.tid WHERE l.label = 'Tavolo 1'),
     prod1 AS (SELECT id, name, price FROM tenant_products WHERE sku = 'DEMO-C01'),
     prod2 AS (SELECT id, name, price FROM tenant_products WHERE sku = 'DEMO-C02'),
     prod3 AS (SELECT id, name, price FROM tenant_products WHERE sku = 'DEMO-B01'),
     prod4 AS (SELECT id, name, price FROM tenant_products WHERE sku = 'DEMO-S01'),
     ins AS (
       INSERT INTO orders (tenant_id, location_id, location_label_snapshot, area_name_snapshot,
                           source, status, payment_status, subtotal_amount, total_amount,
                           accepted_at, ready_at, delivered_at, created_at, updated_at, tenant_seq)
       VALUES
         ((SELECT tid FROM demo), (SELECT id FROM loc1), 'Ombrellone 1', 'Spiaggia',
          'QR', 'DELIVERED', 'NONE', 16.00, 16.00,
          now()-interval '2h', now()-interval '1h45m', now()-interval '1h30m',
          now()-interval '2h', now()-interval '1h30m', 1),
         ((SELECT tid FROM demo), (SELECT id FROM loc2), 'Ombrellone 3', 'Spiaggia',
          'QR', 'ACCEPTED', 'NONE', 8.50, 8.50,
          now()-interval '10m', NULL, NULL,
          now()-interval '15m', now()-interval '10m', 2),
         ((SELECT tid FROM demo), (SELECT id FROM loc3), 'Tavolo 1', 'Bar',
          'QR', 'NEW', 'NONE', 9.00, 9.00,
          NULL, NULL, NULL,
          now()-interval '3m', now()-interval '3m', 3)
       RETURNING id, tenant_id, status, tenant_seq
     )
INSERT INTO order_items (order_id, tenant_id, tenant_product_id, product_name_snapshot,
                          unit_price_snapshot, quantity, line_total, department_snapshot,
                          created_at, updated_at)
SELECT o.id,
       o.tenant_id,
       p.id,
       p.name,
       p.price,
       CASE WHEN o.tenant_seq = 1 AND p.sku IN ('DEMO-C01') THEN 2
            ELSE 1 END,
       p.price * CASE WHEN o.tenant_seq = 1 AND p.sku IN ('DEMO-C01') THEN 2 ELSE 1 END,
       'BAR',
       now(), now()
FROM ins o
JOIN LATERAL (
  SELECT id, name, price, sku FROM tenant_products
  WHERE sku = CASE
    WHEN o.tenant_seq = 1 THEN 'DEMO-C01'
    WHEN o.tenant_seq = 2 THEN 'DEMO-C03'
    WHEN o.tenant_seq = 3 THEN 'DEMO-C04'
  END
) p ON true;

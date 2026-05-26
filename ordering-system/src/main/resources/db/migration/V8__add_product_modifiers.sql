-- Add modifier groups for product 5 (hmn) in Emergenza Beach tenant

-- Insert Taglia modifier group (size) - radio button (maxSelectable = 1)
INSERT INTO modifier_groups (tenant_id, name, min_selectable, max_selectable, required, status)
SELECT 
    t.id,
    'Taglia' as name,
    1 as min_selectable,
    1 as max_selectable,
    true as required,
    'ACTIVE' as status
FROM tenants t
WHERE t.name = 'Emergenza Beach'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_groups mg 
    WHERE mg.tenant_id = t.id AND mg.name = 'Taglia'
  );

-- Insert Extra modifier group (checkboxes) - maxSelectable = 3
INSERT INTO modifier_groups (tenant_id, name, min_selectable, max_selectable, required, status)
SELECT 
    t.id,
    'Extra' as name,
    0 as min_selectable,
    3 as max_selectable,
    false as required,
    'ACTIVE' as status
FROM tenants t
WHERE t.name = 'Emergenza Beach'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_groups mg 
    WHERE mg.tenant_id = t.id AND mg.name = 'Extra'
  );

-- Insert Taglia options
INSERT INTO modifier_options (modifier_group_id, tenant_id, name, price_delta, status)
SELECT 
    mg.id,
    mg.tenant_id,
    'Piccola' as name,
    0 as price_delta,
    'ACTIVE' as status
FROM modifier_groups mg
WHERE mg.tenant_id = (SELECT id FROM tenants WHERE name = 'Emergenza Beach')
  AND mg.name = 'Taglia'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_options mo 
    WHERE mo.modifier_group_id = mg.id AND mo.name = 'Piccola'
  );

INSERT INTO modifier_options (modifier_group_id, tenant_id, name, price_delta, status)
SELECT 
    mg.id,
    mg.tenant_id,
    'Media' as name,
    50 as price_delta,
    'ACTIVE' as status
FROM modifier_groups mg
WHERE mg.tenant_id = (SELECT id FROM tenants WHERE name = 'Emergenza Beach')
  AND mg.name = 'Taglia'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_options mo 
    WHERE mo.modifier_group_id = mg.id AND mo.name = 'Media'
  );

INSERT INTO modifier_options (modifier_group_id, tenant_id, name, price_delta, status)
SELECT 
    mg.id,
    mg.tenant_id,
    'Grande' as name,
    100 as price_delta,
    'ACTIVE' as status
FROM modifier_groups mg
WHERE mg.tenant_id = (SELECT id FROM tenants WHERE name = 'Emergenza Beach')
  AND mg.name = 'Taglia'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_options mo 
    WHERE mo.modifier_group_id = mg.id AND mo.name = 'Grande'
  );

-- Insert Extra options
INSERT INTO modifier_options (modifier_group_id, tenant_id, name, price_delta, status)
SELECT 
    mg.id,
    mg.tenant_id,
    'Formaggio' as name,
    50 as price_delta,
    'ACTIVE' as status
FROM modifier_groups mg
WHERE mg.tenant_id = (SELECT id FROM tenants WHERE name = 'Emergenza Beach')
  AND mg.name = 'Extra'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_options mo 
    WHERE mo.modifier_group_id = mg.id AND mo.name = 'Formaggio'
  );

INSERT INTO modifier_options (modifier_group_id, tenant_id, name, price_delta, status)
SELECT 
    mg.id,
    mg.tenant_id,
    'Bacon' as name,
    100 as price_delta,
    'ACTIVE' as status
FROM modifier_groups mg
WHERE mg.tenant_id = (SELECT id FROM tenants WHERE name = 'Emergenza Beach')
  AND mg.name = 'Extra'
  AND NOT EXISTS (
    SELECT 1 FROM modifier_options mo 
    WHERE mo.modifier_group_id = mg.id AND mo.name = 'Bacon'
  );

-- Link modifier groups to product 5
-- Product 5 is accessed via tenant_product_id
INSERT INTO tenant_product_modifier_groups (tenant_product_id, modifier_group_id)
SELECT 
    tp.id,
    mg.id
FROM tenant_products tp
JOIN modifier_groups mg ON mg.tenant_id = tp.tenant_id
WHERE tp.product_id = 5
  AND tp.tenant_id = (SELECT id FROM tenants WHERE name = 'Emergenza Beach')
  AND mg.name IN ('Taglia', 'Extra')
  AND NOT EXISTS (
    SELECT 1 FROM tenant_product_modifier_groups tpmg
    WHERE tpmg.tenant_product_id = tp.id AND tpmg.modifier_group_id = mg.id
  );

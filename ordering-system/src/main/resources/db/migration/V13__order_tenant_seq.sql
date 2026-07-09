-- Add per-tenant sequential order number
ALTER TABLE orders ADD COLUMN tenant_seq BIGINT;

-- Backfill existing rows with per-tenant ROW_NUMBER
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS seq
    FROM orders
)
UPDATE orders o
SET tenant_seq = r.seq
FROM ranked r
WHERE o.id = r.id;

-- Make NOT NULL and set default to 0 (service will always provide the value)
ALTER TABLE orders ALTER COLUMN tenant_seq SET NOT NULL;
ALTER TABLE orders ALTER COLUMN tenant_seq SET DEFAULT 0;

CREATE INDEX idx_orders_tenant_seq ON orders(tenant_id, tenant_seq);

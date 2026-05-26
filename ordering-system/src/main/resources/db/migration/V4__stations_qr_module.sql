-- =========================================================
-- 4) STATIONS / QR MODULE
-- =========================================================

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS operational_status dom_status_short NOT NULL DEFAULT 'AVAILABLE';

ALTER TABLE locations
    ADD CONSTRAINT chk_locations_operational_status
        CHECK (operational_status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'ORDERING_DISABLED', 'CLOSED'));

ALTER TABLE location_tokens
    ADD COLUMN IF NOT EXISTS qr_value text NOT NULL DEFAULT '';

ALTER TABLE location_tokens
    ADD COLUMN IF NOT EXISTS image_path text;

ALTER TABLE location_tokens
    ADD COLUMN IF NOT EXISTS generated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE location_tokens
    ADD COLUMN IF NOT EXISTS regenerated_at timestamptz;

CREATE INDEX IF NOT EXISTS idx_locations_tenant_operational_status
    ON locations(tenant_id, operational_status);

CREATE INDEX IF NOT EXISTS idx_location_tokens_tenant_status
    ON location_tokens(tenant_id, status);

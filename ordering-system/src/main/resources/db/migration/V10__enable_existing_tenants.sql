-- Ensure existing tenants remain usable after introducing tenants.enabled.
-- V9 adds the column with default FALSE; for pre-existing rows we enable them.
update tenants set enabled = true where enabled = false;

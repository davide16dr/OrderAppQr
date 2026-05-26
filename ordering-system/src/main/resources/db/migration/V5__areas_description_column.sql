-- =========================================================
-- 5) AREAS DESCRIPTION COLUMN ALIGNMENT
-- =========================================================

ALTER TABLE areas
    ADD COLUMN IF NOT EXISTS description varchar(300);

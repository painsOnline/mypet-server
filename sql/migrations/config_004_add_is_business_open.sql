-- Add is_bussiness_open column to c_tenant
ALTER TABLE c_tenant ADD COLUMN IF NOT EXISTS is_bussiness_open SMALLINT NOT NULL DEFAULT 0;
-- Existing tenants with is_disable=0 are assumed to be open for business
UPDATE c_tenant SET is_bussiness_open = 1 WHERE is_disable = 0;

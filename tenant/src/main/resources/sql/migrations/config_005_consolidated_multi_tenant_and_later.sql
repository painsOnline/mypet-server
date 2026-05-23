-- =====================================================
-- Migration: config_005_consolidated
-- Target: mypet_config
-- Description: Consolidated config migration — tenant admin table, is_business_open
-- Idempotent: yes (IF NOT EXISTS / ON CONFLICT DO NOTHING)
-- =====================================================

-- ============================================================
-- config_002: Tenant admin table + default super admin
-- ============================================================
CREATE TABLE IF NOT EXISTS t_admin (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tenant_admin_account ON t_admin(account);

-- Default super admin (password: mysuper123abc+-)
INSERT INTO t_admin (id, account, password, create_time)
VALUES (gen_random_uuid(), 'super', '0TjtpeCsBG11xWz+7JxkGA==:pDdDQ1ybPeDGz09P1PO8RMH/bb0HRcsSV/S8hkJ/6gg=', now()::timestamp(0))
ON CONFLICT (account) DO NOTHING;

-- ============================================================
-- config_004: is_business_open
-- ============================================================
ALTER TABLE c_tenant ADD COLUMN IF NOT EXISTS is_bussiness_open SMALLINT NOT NULL DEFAULT 0;
UPDATE c_tenant SET is_bussiness_open = 1 WHERE is_disable = 0;

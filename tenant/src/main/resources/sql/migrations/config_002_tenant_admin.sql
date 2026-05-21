-- =====================================================
-- Migration: config_002_tenant_admin
-- Target: mypet_config
-- Description: Add t_admin table for tenant management system admins
-- Idempotent: yes (CREATE IF NOT EXISTS)
-- =====================================================

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

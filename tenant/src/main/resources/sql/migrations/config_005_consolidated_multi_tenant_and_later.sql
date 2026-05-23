-- =====================================================
-- Migration: config_005_consolidated
-- Target: mypet_config
-- Description: Consolidated config migration — migration log table, tenant admin table,
--              is_business_open, fix security tables (remove tenant_code per database.md)
-- Idempotent: yes (IF NOT EXISTS / IF EXISTS / ON CONFLICT DO NOTHING)
-- =====================================================

-- ============================================================
-- Migration log table (if not yet created)
-- ============================================================
CREATE TABLE IF NOT EXISTS c_migration_log (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(100) NOT NULL UNIQUE,
    migration_desc VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'wait',
    result TEXT,
    exec_time TIMESTAMP,
    exec_end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- ============================================================
-- Fix: Drop old security tables with tenant_code, recreate per database.md (NO tenant_code)
-- ============================================================
DROP TABLE IF EXISTS c_admin_login_error_log;
CREATE TABLE IF NOT EXISTS c_admin_login_error_log (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    login_ip VARCHAR(100) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alert_account ON c_admin_login_error_log(account);
CREATE INDEX IF NOT EXISTS idx_alert_time ON c_admin_login_error_log(create_time);

DROP TABLE IF EXISTS c_admin_login_lock;
CREATE TABLE IF NOT EXISTS c_admin_login_lock (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    lock_end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_allock_account ON c_admin_login_lock(account);

-- ============================================================
-- Tenant admin table + default super admin
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
-- is_business_open on c_tenant
-- ============================================================
ALTER TABLE c_tenant ADD COLUMN IF NOT EXISTS is_bussiness_open SMALLINT NOT NULL DEFAULT 0;
UPDATE c_tenant SET is_bussiness_open = 1 WHERE is_disable = 0;

-- =====================================================
-- File: init-config.sql
-- Author: system
-- Date: 2026-05-10
-- Description: Initialize mypet_config database (v2.0)
-- =====================================================

-- c_database_instance: database instance connection info
CREATE TABLE IF NOT EXISTS c_database_instance (
    id CHAR(36) PRIMARY KEY,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    "user" VARCHAR(255) NOT NULL,
    "password" VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- c_tenant: tenant registry
CREATE TABLE IF NOT EXISTS c_tenant (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    database_instance_id CHAR(36) NOT NULL,
    free_shipping_amount NUMERIC(8,2) NOT NULL DEFAULT 20.00,
    is_disable SMALLINT NOT NULL DEFAULT 0,
    is_bussiness_open SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tenant_code ON c_tenant(code);

-- Insert default database instance (password AES-encrypted)
INSERT INTO c_database_instance (id, host, port, "user", "password", create_time)
VALUES ('00000000-0000-0000-0000-000000000001', '127.0.0.1', 1800, 'postgres', 'UfqGmZLESD2YqLqEeDV1KQ==', now()::timestamp(0))
ON CONFLICT (id) DO NOTHING;

-- Insert default tenant "xlong"
INSERT INTO c_tenant (id, code, name, database_instance_id, free_shipping_amount, is_disable, is_bussiness_open, create_time)
VALUES ('00000000-0000-0000-0000-000000000010', 'xlong', '鑫钱猫惠州分店', '00000000-0000-0000-0000-000000000001', 20.00, 0, 1, now()::timestamp(0))
ON CONFLICT (id) DO NOTHING;

-- c_admin_login_error_log: login failure tracking
CREATE TABLE IF NOT EXISTS c_admin_login_error_log (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    login_ip VARCHAR(100) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_login_error_account ON c_admin_login_error_log(account);
CREATE INDEX IF NOT EXISTS idx_login_error_time ON c_admin_login_error_log(create_time);

-- c_admin_login_lock: account lock records
CREATE TABLE IF NOT EXISTS c_admin_login_lock (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    lock_end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_login_lock_account ON c_admin_login_lock(account);

-- t_admin: tenant management system admin users
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

-- c_migration_log: migration execution tracking
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

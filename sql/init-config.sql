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
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tenant_code ON c_tenant(code);

-- Insert default database instance (password AES-encrypted)
INSERT INTO c_database_instance (id, host, port, "user", "password", create_time)
VALUES ('00000000-0000-0000-0000-000000000001', '127.0.0.1', 1800, 'postgres', 'UfqGmZLESD2YqLqEeDV1KQ==', now()::timestamp(0))
ON CONFLICT (id) DO NOTHING;

-- Insert default tenant "xlong"
INSERT INTO c_tenant (id, code, name, database_instance_id, free_shipping_amount, is_disable, create_time)
VALUES ('00000000-0000-0000-0000-000000000010', 'xlong', '鑫钱猫惠州分店', '00000000-0000-0000-0000-000000000001', 20.00, 0, now()::timestamp(0))
ON CONFLICT (id) DO NOTHING;

-- c_admin_login_error_log: login failure tracking
CREATE TABLE IF NOT EXISTS c_admin_login_error_log (
    id CHAR(36) PRIMARY KEY,
    tenant_code VARCHAR(255) NOT NULL,
    account VARCHAR(255) NOT NULL,
    error_type VARCHAR(50) NOT NULL,
    login_ip VARCHAR(100) NOT NULL DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0)
);
CREATE INDEX IF NOT EXISTS idx_login_error_tenant_account ON c_admin_login_error_log(tenant_code, account);
CREATE INDEX IF NOT EXISTS idx_login_error_time ON c_admin_login_error_log(create_time);

-- c_admin_login_lock: account lock records
CREATE TABLE IF NOT EXISTS c_admin_login_lock (
    id CHAR(36) PRIMARY KEY,
    tenant_code VARCHAR(255) NOT NULL,
    account VARCHAR(255) NOT NULL,
    lock_end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_login_lock_tenant_account ON c_admin_login_lock(tenant_code, account);

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

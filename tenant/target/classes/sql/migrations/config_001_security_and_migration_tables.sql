-- =====================================================
-- Migration: config_001_security_and_migration_tables
-- Target: mypet_config
-- Description: Add login security tables (NO tenant_code per database.md)
--              and migration log table
-- Idempotent: yes (all CREATE IF NOT EXISTS)
-- =====================================================

CREATE TABLE IF NOT EXISTS c_admin_login_error_log (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    login_ip VARCHAR(100) NOT NULL DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alert_account ON c_admin_login_error_log(account);
CREATE INDEX IF NOT EXISTS idx_alert_time ON c_admin_login_error_log(create_time);

CREATE TABLE IF NOT EXISTS c_admin_login_lock (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    lock_end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_allock_account ON c_admin_login_lock(account);

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

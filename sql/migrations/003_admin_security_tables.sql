-- =====================================================
-- Migration: 003_admin_security_tables
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Add admin login error log and lock tables for shop admin risk control
-- Idempotent: yes (all CREATE IF NOT EXISTS)
-- =====================================================

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

CREATE TABLE IF NOT EXISTS c_admin_login_lock (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    lock_end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_allock_account ON c_admin_login_lock(account);

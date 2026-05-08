-- =====================================================
-- File: init-config.sql
-- Author: system
-- Date: 2026-05-03
-- Description: Initialize mypet_config database
-- =====================================================

-- Create config database (run manually as superuser)
-- CREATE DATABASE mypet_config;

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

-- Insert default database instance (password matches local PostgreSQL 18 config)
INSERT INTO c_database_instance (id, host, port, "user", "password", create_time)
VALUES ('00000000-0000-0000-0000-000000000001', '127.0.0.1', 1800, 'postgres', 'mypg123abc', now()::timestamp(0))
ON CONFLICT (id) DO NOTHING;

-- Insert default tenant "xlong"
INSERT INTO c_tenant (id, code, name, database_instance_id, free_shipping_amount, is_disable, create_time)
VALUES ('00000000-0000-0000-0000-000000000010', 'xlong', 'xlong宠物社区私域', '00000000-0000-0000-0000-000000000001', 20.00, 0, now()::timestamp(0))
ON CONFLICT (id) DO NOTHING;


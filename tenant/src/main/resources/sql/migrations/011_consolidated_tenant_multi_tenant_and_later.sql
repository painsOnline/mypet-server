-- =====================================================
-- Migration: 011_consolidated
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Consolidated migration — multi-tenant security, search optimization,
--              shop detail & contact. All idempotent (IF NOT EXISTS / IF EXISTS).
-- =====================================================

-- ============================================================
-- 003: Admin login security tables
-- ============================================================
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

-- ============================================================
-- 005: Full-text search extensions, column, indexes
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pg_jieba;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE t_product ADD COLUMN IF NOT EXISTS search_text TEXT;

DROP INDEX IF EXISTS idx_product_search_text_jieba;
CREATE INDEX IF NOT EXISTS idx_product_search_text_jieba ON t_product USING GIN (to_tsvector('jiebacfg', search_text));
CREATE INDEX IF NOT EXISTS idx_product_search_text_trgm ON t_product USING GIN (search_text gin_trgm_ops);

-- ============================================================
-- 006: Product name indexes
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_product_name ON t_product(name);
CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON t_product USING GIN (name gin_trgm_ops);

-- ============================================================
-- 007: Backfill search_text for existing products
-- ============================================================
UPDATE t_product
SET search_text = TRIM(
  COALESCE(name, '') || ' ' ||
  COALESCE("desc", '') || ' ' ||
  COALESCE(REGEXP_REPLACE(COALESCE(detail, ''), '<[^>]+>', '', 'g'), '')
)
WHERE search_text IS NULL;

-- ============================================================
-- 009/010: Shop detail + contact
-- ============================================================
ALTER TABLE t_shop ADD COLUMN IF NOT EXISTS detail TEXT;
ALTER TABLE t_shop ADD COLUMN IF NOT EXISTS contact VARCHAR(100);

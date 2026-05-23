-- =====================================================
-- Migration: 006_product_name_index
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Add btree index and trigram GIN index on t_product.name
--              for product name LIKE fuzzy matching fallback
-- Idempotent: yes (IF NOT EXISTS)
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_product_name ON t_product(name);
CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON t_product USING GIN (name gin_trgm_ops);

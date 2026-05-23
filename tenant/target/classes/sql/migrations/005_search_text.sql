-- =====================================================
-- Migration: 005_search_text
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Enable pg_jieba & pg_trgm extensions, add search_text column
--              with GIN (jieba) and trigram indexes for full-text search
-- Idempotent: yes (IF NOT EXISTS / IF EXISTS)
-- =====================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS pg_jieba;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add search_text column
ALTER TABLE t_product ADD COLUMN IF NOT EXISTS search_text TEXT;

-- GIN index for jieba full-text search
CREATE INDEX IF NOT EXISTS idx_product_search_text_jieba ON t_product USING GIN (to_tsvector('jieba', search_text));

-- GIN trigram index for pg_trgm fuzzy fallback
CREATE INDEX IF NOT EXISTS idx_product_search_text_trgm ON t_product USING GIN (search_text gin_trgm_ops);

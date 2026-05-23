-- =====================================================
-- Migration: 008_fix_jieba_config
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Rebuild jieba GIN index with correct config name 'jiebacfg'
-- Idempotent: yes (IF EXISTS / IF NOT EXISTS)
-- =====================================================

DROP INDEX IF EXISTS idx_product_search_text_jieba;
CREATE INDEX IF NOT EXISTS idx_product_search_text_jieba ON t_product USING GIN (to_tsvector('jiebacfg', search_text));

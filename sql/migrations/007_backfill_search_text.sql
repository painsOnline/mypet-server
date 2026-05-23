-- =====================================================
-- Migration: 007_backfill_search_text
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Backfill search_text for existing products (name + desc + detail)
--              Full content (category, brand, properties, SKUs) will be
--              populated on next product edit via buildSearchText in ProductService
-- Idempotent: yes (only updates rows with NULL search_text)
-- =====================================================

UPDATE t_product
SET search_text = TRIM(
  COALESCE(name, '') || ' ' ||
  COALESCE("desc", '') || ' ' ||
  COALESCE(REGEXP_REPLACE(COALESCE(detail, ''), '<[^>]+>', '', 'g'), '')
)
WHERE search_text IS NULL;

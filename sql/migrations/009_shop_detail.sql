-- =====================================================
-- Migration: 009_shop_detail
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Add detail column to t_shop for rich-text shop description
-- Idempotent: yes (IF NOT EXISTS)
-- =====================================================

ALTER TABLE t_shop ADD COLUMN IF NOT EXISTS detail TEXT;

-- =====================================================
-- Migration: 001_add_seller_message
-- Target: mypet_{tenantCode} (tenant business DB)
-- Description: Add seller_message column to t_order
-- Idempotent: yes
-- =====================================================

ALTER TABLE t_order ADD COLUMN IF NOT EXISTS seller_message TEXT;

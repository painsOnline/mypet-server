-- =====================================================
-- Migration: 012_add_virtual_inventory
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Add virtual_inventory column to t_product_sku.
--              virtual_inventory is used for mini-program display/order;
--              inventory (real) is only deducted on admin dispatch.
--              Initialize with random values 20-100 for existing data.
-- =====================================================

ALTER TABLE t_product_sku ADD COLUMN IF NOT EXISTS virtual_inventory INT NOT NULL DEFAULT 0;

-- Initialize virtual_inventory for existing rows with random values 20-100
UPDATE t_product_sku SET virtual_inventory = FLOOR(RANDOM() * 81 + 20)::INT
WHERE virtual_inventory = 0 OR virtual_inventory IS NULL;

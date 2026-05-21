-- ============================================================
-- 002b_finalize: Steps 7-12 (validate, swap, indexes, cleanup)
-- Run after BatchMigrateSpecs completes successfully.
-- Idempotent.
-- ============================================================

-- STEP 7: 校验 + 换名（合并到同一 DO 块，校验失败不执行 DROP）
-- 校验内容：1. 行数一致  2. 无空数组 []（说明有值未匹配上 t_product_specs_value）
DO $$
DECLARE old_cnt INT; new_cnt INT; empty_cnt INT;
BEGIN
    SELECT COUNT(*) INTO old_cnt FROM t_product_sku WHERE specs IS NOT NULL;
    SELECT COUNT(*) INTO new_cnt FROM t_product_sku WHERE specs_new IS NOT NULL;
    SELECT COUNT(*) INTO empty_cnt FROM t_product_sku WHERE specs IS NOT NULL AND specs_new = '[]'::jsonb;
    IF old_cnt != new_cnt THEN
        RAISE EXCEPTION 't_product_sku: specs_new(%) != specs(%), abort', new_cnt, old_cnt;
    END IF;
    IF empty_cnt > 0 THEN
        RAISE EXCEPTION 't_product_sku: % rows have empty specs_new (values not in t_product_specs_value), abort', empty_cnt;
    END IF;
    ALTER TABLE t_product_sku DROP COLUMN specs;
    ALTER TABLE t_product_sku RENAME COLUMN specs_new TO specs;
END $$;

DO $$
DECLARE old_cnt INT; new_cnt INT; empty_cnt INT;
BEGIN
    SELECT COUNT(*) INTO old_cnt FROM t_order_product_skus WHERE specs IS NOT NULL;
    SELECT COUNT(*) INTO new_cnt FROM t_order_product_skus WHERE specs_new IS NOT NULL;
    SELECT COUNT(*) INTO empty_cnt FROM t_order_product_skus WHERE specs IS NOT NULL AND specs_new = '[]'::jsonb;
    IF old_cnt != new_cnt THEN
        RAISE EXCEPTION 't_order_product_skus: specs_new(%) != specs(%), abort', new_cnt, old_cnt;
    END IF;
    IF empty_cnt > 0 THEN
        RAISE EXCEPTION 't_order_product_skus: % rows have empty specs_new, abort', empty_cnt;
    END IF;
    ALTER TABLE t_order_product_skus DROP COLUMN specs;
    ALTER TABLE t_order_product_skus RENAME COLUMN specs_new TO specs;
END $$;

DO $$
DECLARE old_cnt INT; new_cnt INT; empty_cnt INT;
BEGIN
    SELECT COUNT(*) INTO old_cnt FROM t_cart WHERE specs IS NOT NULL;
    SELECT COUNT(*) INTO new_cnt FROM t_cart WHERE specs_new IS NOT NULL;
    SELECT COUNT(*) INTO empty_cnt FROM t_cart WHERE specs IS NOT NULL AND specs_new = '[]'::jsonb;
    IF old_cnt != new_cnt THEN
        RAISE EXCEPTION 't_cart: specs_new(%) != specs(%), abort', new_cnt, old_cnt;
    END IF;
    IF empty_cnt > 0 THEN
        RAISE EXCEPTION 't_cart: % rows have empty specs_new, abort', empty_cnt;
    END IF;
    ALTER TABLE t_cart DROP COLUMN specs;
    ALTER TABLE t_cart RENAME COLUMN specs_new TO specs;
END $$;

-- STEP 8: GIN 索引 (specs 列此时已是 JSONB)
CREATE INDEX IF NOT EXISTS idx_sku_specs_spec_id ON t_product_sku USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_sku_specs_value_id ON t_product_sku USING GIN ((specs->'value_id'));
CREATE INDEX IF NOT EXISTS idx_ops_specs_spec_id ON t_order_product_skus USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_ops_specs_value_id ON t_order_product_skus USING GIN ((specs->'value_id'));
CREATE INDEX IF NOT EXISTS idx_cart_specs_spec_id ON t_cart USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_cart_specs_value_id ON t_cart USING GIN ((specs->'value_id'));

-- STEP 9-11: 订单属性表 value_id 迁移 + value_name 修正
ALTER TABLE t_order_product_properties ADD COLUMN IF NOT EXISTS value_id uuid;
CREATE INDEX IF NOT EXISTS idx_opp_value_id ON t_order_product_properties(value_id);

UPDATE t_order_product_properties opp SET value_id = psv.id
FROM t_product_specs_value psv JOIN t_product_specs ps ON psv.specs_id = ps.id
WHERE ps.name = opp.name AND psv.value_name = opp.value_name
  AND opp.value_id IS NULL AND opp.value_name IS NOT NULL AND opp.value_name != '';

-- Fallback: 精确匹配失败时取该规格第一个可用 value_id（按属性名匹配规格名）
UPDATE t_order_product_properties opp SET value_id = sub.first_val_id
FROM (
    SELECT DISTINCT ON (psv.specs_id) ps.name AS spec_name, psv.id AS first_val_id
    FROM t_product_specs_value psv JOIN t_product_specs ps ON ps.id = psv.specs_id
    ORDER BY psv.specs_id, psv.sort
) sub
WHERE opp.name = sub.spec_name AND opp.value_id IS NULL;

-- Set NOT NULL after data is populated
ALTER TABLE t_order_product_properties ALTER COLUMN value_id SET NOT NULL;

UPDATE t_order_product_properties opp SET value_name = NULL
FROM t_product_specs ps
WHERE ps.name = opp.name AND ps.input_type IS DISTINCT FROM 1
  AND opp.value_name IS NOT NULL AND opp.value_id IS NOT NULL;

-- STEP 11.5: 修复 CTAS 导致丢失的 NOT NULL 约束和默认值
UPDATE t_product_sku SET is_delete = 0 WHERE is_delete IS NULL;
ALTER TABLE t_product_sku ALTER COLUMN is_delete SET DEFAULT 0;
ALTER TABLE t_product_sku ALTER COLUMN is_delete SET NOT NULL;

UPDATE t_product_category SET is_delete = 0 WHERE is_delete IS NULL;
ALTER TABLE t_product_category ALTER COLUMN is_delete SET DEFAULT 0;
ALTER TABLE t_product_category ALTER COLUMN is_delete SET NOT NULL;

UPDATE t_product_type SET is_delete = 0 WHERE is_delete IS NULL;
ALTER TABLE t_product_type ALTER COLUMN is_delete SET DEFAULT 0;
ALTER TABLE t_product_type ALTER COLUMN is_delete SET NOT NULL;

UPDATE t_product_brand SET is_delete = 0 WHERE is_delete IS NULL;
ALTER TABLE t_product_brand ALTER COLUMN is_delete SET DEFAULT 0;
ALTER TABLE t_product_brand ALTER COLUMN is_delete SET NOT NULL;

UPDATE t_product_properties SET is_delete = 0 WHERE is_delete IS NULL;
ALTER TABLE t_product_properties ALTER COLUMN is_delete SET DEFAULT 0;
ALTER TABLE t_product_properties ALTER COLUMN is_delete SET NOT NULL;

UPDATE t_order SET is_delete = 0 WHERE is_delete IS NULL;
ALTER TABLE t_order ALTER COLUMN is_delete SET DEFAULT 0;
ALTER TABLE t_order ALTER COLUMN is_delete SET NOT NULL;

UPDATE t_product SET is_enable = 1 WHERE is_enable IS NULL;
ALTER TABLE t_product ALTER COLUMN is_enable SET DEFAULT 1;
ALTER TABLE t_product ALTER COLUMN is_enable SET NOT NULL;

UPDATE t_cart SET selected = 1 WHERE selected IS NULL;
ALTER TABLE t_cart ALTER COLUMN selected SET DEFAULT 1;
ALTER TABLE t_cart ALTER COLUMN selected SET NOT NULL;

UPDATE t_receiver SET is_default = 0 WHERE is_default IS NULL;
ALTER TABLE t_receiver ALTER COLUMN is_default SET DEFAULT 0;
ALTER TABLE t_receiver ALTER COLUMN is_default SET NOT NULL;

ALTER TABLE t_product ALTER COLUMN detail DROP NOT NULL;

-- STEP 12: 删除废弃字段
DO $$
DECLARE unmigrated_count INT;
BEGIN
    SELECT COUNT(*) INTO unmigrated_count FROM t_product_specs ps
    WHERE ps.input_options IS NOT NULL AND array_length(ps.input_options, 1) > 0
      AND NOT EXISTS (SELECT 1 FROM t_product_specs_value psv WHERE psv.specs_id = ps.id);
    IF unmigrated_count = 0 THEN
        ALTER TABLE t_product_specs DROP COLUMN IF EXISTS input_options;
    ELSE
        RAISE WARNING 'Cannot drop input_options: % specs still have unmigrated values', unmigrated_count;
    END IF;
END $$;

ALTER TABLE t_cart DROP COLUMN IF EXISTS price;
ALTER TABLE t_cart DROP COLUMN IF EXISTS now_price;

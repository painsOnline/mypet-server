-- ============================================================
-- 002a_setup: Steps 0-5 (DDL + light data migration)
-- Idempotent. Run before BatchMigrateSpecs.
-- ============================================================

-- STEP 0: 备份全部需要改动的表
CREATE TABLE IF NOT EXISTS bak_t_product_specs_20260516 (LIKE t_product_specs INCLUDING ALL);
INSERT INTO bak_t_product_specs_20260516 SELECT * FROM t_product_specs ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bak_t_product_properties_20260516 (LIKE t_product_properties INCLUDING ALL);
INSERT INTO bak_t_product_properties_20260516 SELECT * FROM t_product_properties ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bak_t_product_sku_20260516 (LIKE t_product_sku INCLUDING ALL);
INSERT INTO bak_t_product_sku_20260516 SELECT * FROM t_product_sku ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bak_t_order_product_skus_20260516 (LIKE t_order_product_skus INCLUDING ALL);
INSERT INTO bak_t_order_product_skus_20260516 SELECT * FROM t_order_product_skus ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS bak_t_order_product_properties_20260516 (LIKE t_order_product_properties INCLUDING ALL);
INSERT INTO bak_t_order_product_properties_20260516 SELECT * FROM t_order_product_properties ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bak_t_cart_20260516 (LIKE t_cart INCLUDING ALL);
INSERT INTO bak_t_cart_20260516 SELECT * FROM t_cart ON CONFLICT DO NOTHING;

-- STEP 1: 新建规格值表 t_product_specs_value
CREATE TABLE IF NOT EXISTS t_product_specs_value (
    id CHAR(36) PRIMARY KEY,
    specs_id CHAR(36) NOT NULL,
    value_name VARCHAR(255) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_psv_specs_id ON t_product_specs_value(specs_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_psv_specsid_value ON t_product_specs_value(specs_id, value_name);

-- STEP 2: 迁移 input_options -> t_product_specs_value
DO $$
DECLARE
    spec RECORD; val TEXT; idx INT;
BEGIN
    FOR spec IN
        SELECT id, input_options FROM t_product_specs
        WHERE input_options IS NOT NULL AND array_length(input_options, 1) > 0
          AND NOT EXISTS (SELECT 1 FROM t_product_specs_value psv WHERE psv.specs_id = t_product_specs.id)
    LOOP
        idx := 0;
        FOREACH val IN ARRAY spec.input_options LOOP
            INSERT INTO t_product_specs_value (id, specs_id, value_name, sort, create_time)
            VALUES (gen_random_uuid(), spec.id, val, idx, now()::timestamp(0))
            ON CONFLICT (specs_id, value_name) DO NOTHING;
            idx := idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- STEP 3: 属性表加 value_id 并迁移数据
ALTER TABLE t_product_properties ADD COLUMN IF NOT EXISTS value_id CHAR(36);
CREATE INDEX IF NOT EXISTS idx_pp_value_id ON t_product_properties(value_id);

UPDATE t_product_properties pp SET value_id = psv.id
FROM t_product_specs_value psv
WHERE pp.specs_id = psv.specs_id AND pp.value_name = psv.value_name
  AND pp.value_id IS NULL AND pp.value_name IS NOT NULL AND pp.value_name != '';

-- Fallback: 对于 input_type=1 的规格，若精确 value_name 匹配失败，
-- 取其第一个可用 value_id（这是之前设计缺陷导致 value_name 不一致的补偿）
UPDATE t_product_properties pp SET value_id = sub.first_val_id
FROM (
    SELECT DISTINCT ON (psv.specs_id) psv.specs_id, psv.id AS first_val_id
    FROM t_product_specs_value psv
    JOIN t_product_specs ps ON ps.id = psv.specs_id
    WHERE ps.input_type = 1
    ORDER BY psv.specs_id, psv.sort
) sub
WHERE pp.specs_id = sub.specs_id AND pp.value_id IS NULL;

-- STEP 4: 属性表修正 value_name 逻辑（仅唯一值规格保留）
ALTER TABLE t_product_properties ALTER COLUMN value_name DROP NOT NULL;
ALTER TABLE t_order_product_properties ALTER COLUMN value_name DROP NOT NULL;
UPDATE t_product_properties pp SET value_name = NULL
FROM t_product_specs ps
WHERE pp.specs_id = ps.id AND ps.input_type IS DISTINCT FROM 1 AND pp.value_name IS NOT NULL;

-- STEP 5: 三张表新增 specs_new (JSONB) + cart.product_id
ALTER TABLE t_product_sku ADD COLUMN IF NOT EXISTS specs_new JSONB;
ALTER TABLE t_order_product_skus ADD COLUMN IF NOT EXISTS specs_new JSONB;
ALTER TABLE t_cart ADD COLUMN IF NOT EXISTS specs_new JSONB;
ALTER TABLE t_cart ADD COLUMN IF NOT EXISTS product_id CHAR(36);

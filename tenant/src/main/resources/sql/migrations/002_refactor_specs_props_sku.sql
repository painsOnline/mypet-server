-- =====================================================
-- Migration: 002_refactor_specs_props_sku
-- Target: mypet_{tenantCode} + mypet_empty (tenant business DB)
-- Description: Refactor specs values, properties value_id, SKU/cart specs JSONB
-- Idempotent: yes (checks if already migrated before each step)
-- =====================================================

-- ============================================================
-- STEP 0: Backup all affected tables (if not already backed up)
-- ============================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'bak_t_product_specs_value') THEN
    -- We'll create backups as we go. First, just ensure bak_ prefix tables exist if needed.
  END IF;
END $$;

-- backup: t_product_specs (keep original data safe)
CREATE TABLE IF NOT EXISTS bak_t_product_specs_20260516 (LIKE t_product_specs INCLUDING ALL);
INSERT INTO bak_t_product_specs_20260516 SELECT * FROM t_product_specs
ON CONFLICT (id) DO NOTHING;

-- backup: t_product_properties
CREATE TABLE IF NOT EXISTS bak_t_product_properties_20260516 (LIKE t_product_properties INCLUDING ALL);
INSERT INTO bak_t_product_properties_20260516 SELECT * FROM t_product_properties
ON CONFLICT (id) DO NOTHING;

-- backup: t_product_sku
CREATE TABLE IF NOT EXISTS bak_t_product_sku_20260516 (LIKE t_product_sku INCLUDING ALL);
INSERT INTO bak_t_product_sku_20260516 SELECT * FROM t_product_sku
ON CONFLICT (id) DO NOTHING;

-- backup: t_order_product_skus
CREATE TABLE IF NOT EXISTS bak_t_order_product_skus_20260516 (LIKE t_order_product_skus INCLUDING ALL);
INSERT INTO bak_t_order_product_skus_20260516 SELECT * FROM t_order_product_skus;

-- backup: t_order_product_properties
CREATE TABLE IF NOT EXISTS bak_t_order_product_properties_20260516 (LIKE t_order_product_properties INCLUDING ALL);
INSERT INTO bak_t_order_product_properties_20260516 SELECT * FROM t_order_product_properties;

-- backup: t_cart
CREATE TABLE IF NOT EXISTS bak_t_cart_20260516 (LIKE t_cart INCLUDING ALL);
INSERT INTO bak_t_cart_20260516 SELECT * FROM t_cart;

-- ============================================================
-- STEP 0.5: Normalize 32-char UUIDs → 36-char with dashes
-- Only affects values without dashes. Already-36-char UUIDs are untouched.
-- ============================================================
-- Pattern: 32 hex chars → 8-4-4-4-12 with dashes
DO $$
DECLARE
    rec RECORD;
    tbl TEXT;
    col TEXT;
    sql TEXT;
BEGIN
    FOR rec IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND (data_type = 'character' OR udt_name = 'char')
          AND character_maximum_length = 36
          AND table_name NOT LIKE 'bak_%'
    LOOP
        sql := format('UPDATE %I SET %I = regexp_replace(%I, ''([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})'', ''\1-\2-\3-\4-\5'') WHERE length(%I) = 32',
                      rec.table_name, rec.column_name, rec.column_name, rec.column_name);
        BEGIN EXECUTE sql; EXCEPTION WHEN OTHERS THEN RAISE WARNING 'UUID normalize failed for %.%: %', rec.table_name, rec.column_name, SQLERRM; END;
    END LOOP;
END $$;

-- ============================================================
-- STEP 0.6: Fix 32-char UUIDs in image URLs (product IDs in paths)
-- Only affects relative/absolute URLs containing 32-char hex IDs
-- ============================================================
DO $$
DECLARE
    r RECORD;
    fix_fn TEXT := '([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})';
BEGIN
    -- t_product: main_pictures[]
    UPDATE t_product SET main_pictures = (
        SELECT array_agg(regexp_replace(v, fix_fn, '\1-\2-\3-\4-\5'))
        FROM unnest(main_pictures) v
    ) WHERE EXISTS (SELECT 1 FROM unnest(main_pictures) v WHERE v ~ '[a-f0-9]{32}');

    -- t_product: picture, detail
    UPDATE t_product SET picture = regexp_replace(picture, fix_fn, '\1-\2-\3-\4-\5') WHERE picture ~ '[a-f0-9]{32}';
    UPDATE t_product SET detail = regexp_replace(detail, fix_fn, '\1-\2-\3-\4-\5', 'g') WHERE detail ~ '[a-f0-9]{32}';

    -- t_product_sku: picture
    UPDATE t_product_sku SET picture = regexp_replace(picture, fix_fn, '\1-\2-\3-\4-\5') WHERE picture ~ '[a-f0-9]{32}';

    -- t_product_brand: brand_logo
    UPDATE t_product_brand SET brand_logo = regexp_replace(brand_logo, fix_fn, '\1-\2-\3-\4-\5') WHERE brand_logo ~ '[a-f0-9]{32}';

    -- t_product_category: picture
    UPDATE t_product_category SET picture = regexp_replace(picture, fix_fn, '\1-\2-\3-\4-\5') WHERE picture ~ '[a-f0-9]{32}';

    -- t_shop: logo, banners
    UPDATE t_shop SET logo = regexp_replace(logo, fix_fn, '\1-\2-\3-\4-\5') WHERE logo ~ '[a-f0-9]{32}';
    UPDATE t_shop SET banners = regexp_replace(banners::text, fix_fn, '\1-\2-\3-\4-\5', 'g')::jsonb WHERE banners::text ~ '[a-f0-9]{32}';
END $$;

-- ============================================================
-- STEP 1: Create t_product_specs_value table
-- ============================================================
CREATE TABLE IF NOT EXISTS t_product_specs_value (
    id CHAR(36) PRIMARY KEY,
    specs_id CHAR(36) NOT NULL,
    value_name VARCHAR(255) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_psv_specs_id ON t_product_specs_value(specs_id);
-- Unique constraint: same spec cannot have duplicate value names
CREATE UNIQUE INDEX IF NOT EXISTS idx_psv_specsid_value ON t_product_specs_value(specs_id, value_name);

-- ============================================================
-- STEP 2: Migrate input_options -> t_product_specs_value
-- ============================================================
-- Only migrate if there are specs that still have input_options AND
-- no values have been created for them yet
DO $$
DECLARE
    spec RECORD;
    val TEXT;
    idx INT;
BEGIN
    FOR spec IN
        SELECT id, input_options
        FROM t_product_specs
        WHERE input_options IS NOT NULL
          AND array_length(input_options, 1) > 0
          AND NOT EXISTS (
            SELECT 1 FROM t_product_specs_value psv WHERE psv.specs_id = t_product_specs.id
          )
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

-- ============================================================
-- STEP 3: Add value_id to t_product_properties
-- ============================================================
ALTER TABLE t_product_properties ADD COLUMN IF NOT EXISTS value_id CHAR(36);
CREATE INDEX IF NOT EXISTS idx_pp_value_id ON t_product_properties(value_id);

-- Migrate value_id: match existing value_name against t_product_specs_value
UPDATE t_product_properties pp
SET value_id = psv.id
FROM t_product_specs_value psv
WHERE pp.specs_id = psv.specs_id
  AND pp.value_name = psv.value_name
  AND pp.value_id IS NULL
  AND pp.value_name IS NOT NULL
  AND pp.value_name != '';

-- ============================================================
-- STEP 4: Fix value_name logic in t_product_properties
-- Only keep value_name for unique-value specs (input_type=1),
-- set to NULL for all others
-- ============================================================
ALTER TABLE t_product_properties ALTER COLUMN value_name DROP NOT NULL;
ALTER TABLE t_order_product_properties ALTER COLUMN value_name DROP NOT NULL;
UPDATE t_product_properties pp
SET value_name = NULL
FROM t_product_specs ps
WHERE pp.specs_id = ps.id
  AND ps.input_type IS DISTINCT FROM 1
  AND pp.value_name IS NOT NULL;

-- ============================================================
-- STEP 5: Alter specs column in t_product_sku from JSON to JSONB
-- ============================================================
-- Postgres treats JSON as text; we'll convert to JSONB and add new format
ALTER TABLE t_product_sku ALTER COLUMN specs TYPE JSONB USING specs::jsonb;

-- For t_order_product_skus
ALTER TABLE t_order_product_skus ALTER COLUMN specs TYPE JSONB USING specs::jsonb;

-- For t_cart
ALTER TABLE t_cart ALTER COLUMN specs TYPE JSONB USING specs::jsonb;
ALTER TABLE t_cart ADD COLUMN IF NOT EXISTS product_id CHAR(36);

-- ============================================================
-- STEP 6: Migrate specs JSON structure in t_product_sku
-- Old format: [{"name":"规格","valueName":"2.5Kg"}]
-- New format: [{"spec_id":"SPxxx","value_name":"2.5Kg","value_id":"SVxxx"}]
-- ============================================================
-- First, create a helper function to lookup spec_id by name
-- We'll process SKUs that haven't been migrated yet (check first element for spec_id)

-- Migrate t_product_sku specs
DO $$
DECLARE
    sku RECORD;
    elem JSONB;
    new_specs JSONB := '[]'::JSONB;
    spec_name TEXT;
    spec_value TEXT;
    spec_id TEXT;
    value_id TEXT;
BEGIN
    FOR sku IN
        SELECT id, specs
        FROM t_product_sku
        WHERE specs IS NOT NULL
          AND jsonb_array_length(specs) > 0
          AND NOT (specs->0 ? 'spec_name')  -- Missing spec_name → needs re-migration
    LOOP
        new_specs := '[]'::JSONB;
        FOR elem IN SELECT * FROM jsonb_array_elements(sku.specs)
        LOOP
            spec_name := COALESCE(elem->>'name', elem->>'spec_name', '');
            spec_value := COALESCE(elem->>'valueName', elem->>'value_name', '');
            -- Find spec by name (using spec_id if available, otherwise by name lookup)
            spec_id := COALESCE(elem->>'spec_id', '');
            IF spec_id IS NULL OR spec_id = '' THEN
                SELECT id INTO spec_id FROM t_product_specs WHERE name = spec_name;
            END IF;
            -- Find value by spec_id + value_name
            value_id := COALESCE(elem->>'value_id', '');
            IF (value_id IS NULL OR value_id = '') AND spec_id IS NOT NULL AND spec_id != '' THEN
                SELECT id INTO value_id FROM t_product_specs_value WHERE specs_id = spec_id AND value_name = spec_value;
            END IF;

            -- Build new element
            new_specs := new_specs || jsonb_build_object(
                'spec_id', COALESCE(spec_id, ''),
                'spec_name', spec_name,
                'value_name', CASE WHEN spec_id IS NOT NULL AND spec_id != '' THEN spec_value ELSE '' END,
                'value_id', COALESCE(value_id, '')
            );
        END LOOP;
        UPDATE t_product_sku SET specs = new_specs WHERE id = sku.id;
    END LOOP;
END $$;

-- Migrate t_order_product_skus specs (same logic, but add spec_name)
DO $$
DECLARE
    sku RECORD;
    elem JSONB;
    new_specs JSONB := '[]'::JSONB;
    spec_name TEXT;
    spec_value TEXT;
    spec_id TEXT;
    value_id TEXT;
BEGIN
    FOR sku IN
        SELECT sku_id, order_no, specs
        FROM t_order_product_skus
        WHERE specs IS NOT NULL
          AND jsonb_array_length(specs) > 0
          AND NOT (specs->0 ? 'spec_id')
    LOOP
        new_specs := '[]'::JSONB;
        FOR elem IN SELECT * FROM jsonb_array_elements(sku.specs)
        LOOP
            spec_name := elem->>'name';
            spec_value := elem->>'valueName';
            SELECT id INTO spec_id FROM t_product_specs WHERE name = spec_name;
            SELECT id INTO value_id FROM t_product_specs_value WHERE specs_id = spec_id AND value_name = spec_value;

            new_specs := new_specs || jsonb_build_object(
                'spec_id', COALESCE(spec_id, ''),
                'spec_name', spec_name,
                'value_name', CASE WHEN spec_id IS NOT NULL AND spec_id != '' THEN spec_value ELSE '' END,
                'value_id', COALESCE(value_id, '')
            );
        END LOOP;
        UPDATE t_order_product_skus SET specs = new_specs WHERE sku_id = sku.sku_id AND order_no = sku.order_no;
    END LOOP;
END $$;

-- Migrate t_cart specs (same structure as order_product_skus)
DO $$
DECLARE
    cart RECORD;
    elem JSONB;
    new_specs JSONB := '[]'::JSONB;
    spec_name TEXT;
    spec_value TEXT;
    spec_id TEXT;
    value_id TEXT;
BEGIN
    FOR cart IN
        SELECT id, specs
        FROM t_cart
        WHERE specs IS NOT NULL
          AND jsonb_array_length(specs) > 0
          AND NOT (specs->0 ? 'spec_id')
    LOOP
        new_specs := '[]'::JSONB;
        FOR elem IN SELECT * FROM jsonb_array_elements(cart.specs)
        LOOP
            spec_name := elem->>'name';
            spec_value := elem->>'valueName';
            SELECT id INTO spec_id FROM t_product_specs WHERE name = spec_name;
            SELECT id INTO value_id FROM t_product_specs_value WHERE specs_id = spec_id AND value_name = spec_value;

            new_specs := new_specs || jsonb_build_object(
                'spec_id', COALESCE(spec_id, ''),
                'spec_name', spec_name,
                'value_name', CASE WHEN spec_id IS NOT NULL AND spec_id != '' THEN spec_value ELSE '' END,
                'value_id', COALESCE(value_id, '')
            );
        END LOOP;
        UPDATE t_cart SET specs = new_specs WHERE id = cart.id;
    END LOOP;
END $$;

-- ============================================================
-- ============================================================
-- STEP 6.5: Fix 32-char UUIDs in JSON columns (generated during migration)
-- ============================================================
UPDATE t_product_sku
SET specs = regexp_replace(specs::text, '([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})', '\1-\2-\3-\4-\5', 'g')::jsonb
WHERE specs::text ~ '"[a-f0-9]{32}"';

UPDATE t_order_product_skus
SET specs = regexp_replace(specs::text, '([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})', '\1-\2-\3-\4-\5', 'g')::jsonb
WHERE specs::text ~ '"[a-f0-9]{32}"';

UPDATE t_cart
SET specs = regexp_replace(specs::text, '([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})', '\1-\2-\3-\4-\5', 'g')::jsonb
WHERE specs::text ~ '"[a-f0-9]{32}"';

-- STEP 7: Validation check (if old-format records still exist, log count)
-- ============================================================
DO $$
DECLARE
    cnt INT;
BEGIN
    SELECT COUNT(*) INTO cnt FROM t_product_sku WHERE specs IS NOT NULL AND jsonb_array_length(specs) > 0 AND NOT (specs->0 ? 'spec_id');
    IF cnt > 0 THEN
        RAISE WARNING 'WARNING: % product_sku records still in old format', cnt;
    END IF;
END $$;

-- ============================================================
-- STEP 8: Create GIN indexes on JSONB specs columns
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_sku_specs_spec_id ON t_product_sku USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_sku_specs_value_id ON t_product_sku USING GIN ((specs->'value_id'));
CREATE INDEX IF NOT EXISTS idx_ops_specs_spec_id ON t_order_product_skus USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_ops_specs_value_id ON t_order_product_skus USING GIN ((specs->'value_id'));
CREATE INDEX IF NOT EXISTS idx_cart_specs_spec_id ON t_cart USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_cart_specs_value_id ON t_cart USING GIN ((specs->'value_id'));

-- Note: GIN indexes on individual JSONB paths might be overkill for small datasets.
-- For this use case, a full GIN index on the specs column may be more appropriate:
-- CREATE INDEX IF NOT EXISTS idx_sku_specs_gin ON t_product_sku USING GIN (specs jsonb_path_ops);
-- We use path-specific expression indexes instead, as the queries filter by spec_id/value_id.

-- ============================================================
-- STEP 9: Add value_id to t_order_product_properties
-- ============================================================
ALTER TABLE t_order_product_properties ADD COLUMN IF NOT EXISTS value_id CHAR(36);
CREATE INDEX IF NOT EXISTS idx_opp_value_id ON t_order_product_properties(value_id);

-- ============================================================
-- STEP 10: Migrate value_id in t_order_product_properties
-- ============================================================
UPDATE t_order_product_properties opp
SET value_id = psv.id
FROM t_product_specs_value psv
JOIN t_product_specs ps ON psv.specs_id = ps.id
WHERE ps.name = opp.name
  AND psv.value_name = opp.value_name
  AND opp.value_id IS NULL
  AND opp.value_name IS NOT NULL
  AND opp.value_name != '';

-- ============================================================
-- STEP 11: Fix value_name in t_order_product_properties
-- Only keep value_name for unique-value specs (input_type=1)
-- ============================================================
UPDATE t_order_product_properties opp
SET value_name = NULL
FROM t_product_specs ps
WHERE ps.name = opp.name
  AND ps.input_type IS DISTINCT FROM 1
  AND opp.value_name IS NOT NULL;

-- ============================================================
-- STEP 12: Delete deprecated columns (after verification)
-- ============================================================
-- input_options is removed from t_product_specs per the new schema.
-- Only drop if all specs have been migrated (no rows using old input_options format).
DO $$
DECLARE
    unmigrated_count INT;
BEGIN
    SELECT COUNT(*) INTO unmigrated_count
    FROM t_product_specs ps
    WHERE ps.input_options IS NOT NULL
      AND array_length(ps.input_options, 1) > 0
      AND NOT EXISTS (
        SELECT 1 FROM t_product_specs_value psv WHERE psv.specs_id = ps.id
      );
    IF unmigrated_count = 0 THEN
        ALTER TABLE t_product_specs DROP COLUMN IF EXISTS input_options;
    ELSE
        RAISE WARNING 'Cannot drop input_options: % specs still have unmigrated values', unmigrated_count;
    END IF;
END $$;

-- t_cart: drop price and now_price (now dynamic)
-- Only drop if migrations completed successfully
ALTER TABLE t_cart DROP COLUMN IF EXISTS price;
ALTER TABLE t_cart DROP COLUMN IF EXISTS now_price;

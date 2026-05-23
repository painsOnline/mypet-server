-- =====================================================
-- File: init-empty.sql
-- Author: system
-- Date: 2026-05-13
-- Description: Initialize mypet_empty template database (v3.0)
-- =====================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS pg_jieba;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1. Product Category
CREATE TABLE IF NOT EXISTS t_product_category (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    picture VARCHAR(255),
    sort INT NOT NULL DEFAULT 0,
    is_delete SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_category_is_delete ON t_product_category(is_delete);

-- 2. Product Type
CREATE TABLE IF NOT EXISTS t_product_type (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    is_delete SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_type_is_delete ON t_product_type(is_delete);

-- 3. Product Specs (scope: 0=global, 1=shared, 2=private)
CREATE TABLE IF NOT EXISTS t_product_specs (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type SMALLINT NOT NULL,
    input_type SMALLINT NOT NULL DEFAULT 1,
    "desc" VARCHAR(255),
    scope SMALLINT NOT NULL DEFAULT 2,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_specs_scope ON t_product_specs(scope);
CREATE INDEX IF NOT EXISTS idx_specs_type ON t_product_specs(type);

-- 3a. Product Specs Value
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

-- 3b. Product Type <-> Specs Relation
CREATE TABLE IF NOT EXISTS t_product_type_spec_rel (
    id CHAR(36) PRIMARY KEY,
    product_type CHAR(36) NOT NULL,
    specs_id CHAR(36) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ptsr_product_type ON t_product_type_spec_rel(product_type);
CREATE INDEX IF NOT EXISTS idx_ptsr_specs_id ON t_product_type_spec_rel(specs_id);
CREATE INDEX IF NOT EXISTS idx_ptsr_sort ON t_product_type_spec_rel(sort);

-- 4. Product Brand
CREATE TABLE IF NOT EXISTS t_product_brand (
    id CHAR(36) PRIMARY KEY,
    brand_name VARCHAR(255) NOT NULL,
    brand_en VARCHAR(255),
    brand_logo VARCHAR(255) NOT NULL DEFAULT '',
    brand_desc TEXT,
    sort INT NOT NULL DEFAULT 0,
    is_delete SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_brand_name ON t_product_brand(brand_name);
CREATE INDEX IF NOT EXISTS idx_brand_is_delete ON t_product_brand(is_delete);

-- 5. Product
CREATE TABLE IF NOT EXISTS t_product (
    id CHAR(36) PRIMARY KEY,
    product_type CHAR(36) NOT NULL,
    product_category CHAR(36) NOT NULL,
    product_brand CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    "desc" VARCHAR(255),
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    main_pictures VARCHAR(255)[] NOT NULL,
    picture VARCHAR(255) NOT NULL,
    detail TEXT NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    search_text TEXT,
    is_enable SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_product_product_type ON t_product(product_type);
CREATE INDEX IF NOT EXISTS idx_product_product_category ON t_product(product_category);
CREATE INDEX IF NOT EXISTS idx_product_product_brand ON t_product(product_brand);
CREATE INDEX IF NOT EXISTS idx_product_is_enable ON t_product(is_enable);
CREATE INDEX IF NOT EXISTS idx_product_name ON t_product(name);
CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON t_product USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_product_search_text_jieba ON t_product USING GIN (to_tsvector('jiebacfg', search_text));
CREATE INDEX IF NOT EXISTS idx_product_search_text_trgm ON t_product USING GIN (search_text gin_trgm_ops);

-- 6. Product Properties
CREATE TABLE IF NOT EXISTS t_product_properties (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    specs_id CHAR(36) NOT NULL,
    value_name VARCHAR(255),
    value_id CHAR(36),
    sort INT NOT NULL DEFAULT 0,
    is_delete SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_properties_product_id ON t_product_properties(product_id);
CREATE INDEX IF NOT EXISTS idx_properties_specs_id ON t_product_properties(specs_id);
CREATE INDEX IF NOT EXISTS idx_properties_is_delete ON t_product_properties(is_delete);
CREATE INDEX IF NOT EXISTS idx_properties_value_id ON t_product_properties(value_id);

-- 7. Product SKU
CREATE TABLE IF NOT EXISTS t_product_sku (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    cost_price NUMERIC(8,2) NOT NULL,
    inventory INT NOT NULL DEFAULT 0,
    barcode VARCHAR(255),
    picture VARCHAR(255) NOT NULL,
    specs JSONB NOT NULL DEFAULT '[]'::jsonb,
    sort INT NOT NULL DEFAULT 0,
    is_delete SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sku_product_id ON t_product_sku(product_id);
CREATE INDEX IF NOT EXISTS idx_sku_barcode ON t_product_sku(barcode);
CREATE INDEX IF NOT EXISTS idx_sku_is_delete ON t_product_sku(is_delete);
CREATE INDEX IF NOT EXISTS idx_sku_specs_spec_id ON t_product_sku USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_sku_specs_value_id ON t_product_sku USING GIN ((specs->'value_id'));

-- 8. Inventory Log
CREATE TABLE IF NOT EXISTS t_inventory_log (
    id CHAR(36) PRIMARY KEY,
    sku_id CHAR(36) NOT NULL,
    barcode VARCHAR(100),
    order_no VARCHAR(100),
    change_type VARCHAR(100),
    change_num INT NOT NULL DEFAULT 0,
    before_inventory INT NOT NULL DEFAULT 0,
    after_inventory INT NOT NULL DEFAULT 0,
    operator VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ilog_sku_id ON t_inventory_log(sku_id);
CREATE INDEX IF NOT EXISTS idx_ilog_order_no ON t_inventory_log(order_no);

-- 9. Order
CREATE TABLE IF NOT EXISTS t_order (
    id CHAR(36) PRIMARY KEY,
    member_id CHAR(36) NOT NULL,
    order_no VARCHAR(100) NOT NULL,
    order_type SMALLINT NOT NULL DEFAULT 0,
    order_status SMALLINT NOT NULL DEFAULT 1,
    product_type CHAR(36) NOT NULL,
    total_money NUMERIC(8,2) NOT NULL,
    actual_pay_money NUMERIC(8,2) NOT NULL,
    pay_money NUMERIC(8,2) NOT NULL,
    profit_money NUMERIC(8,2) NOT NULL,
    buyer_message TEXT,
    seller_message TEXT,
    pay_channel SMALLINT NOT NULL DEFAULT 1,
    pay_type SMALLINT NOT NULL DEFAULT 1,
    is_delete SMALLINT NOT NULL DEFAULT 0,
    pay_time TIMESTAMP,
    delivery_time TIMESTAMP,
    receive_time TIMESTAMP,
    finish_time TIMESTAMP,
    cancel_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_order_no ON t_order(order_no);
CREATE INDEX IF NOT EXISTS idx_order_is_delete ON t_order(is_delete);
CREATE INDEX IF NOT EXISTS idx_order_member_id ON t_order(member_id);
CREATE INDEX IF NOT EXISTS idx_order_order_status ON t_order(order_status);
CREATE INDEX IF NOT EXISTS idx_order_product_type ON t_order(product_type);

-- 10. Order Products
CREATE TABLE IF NOT EXISTS t_order_products (
    order_no VARCHAR(100) NOT NULL,
    product_id CHAR(36) NOT NULL,
    product_type CHAR(36) NOT NULL,
    product_category CHAR(36) NOT NULL,
    product_brand CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    "desc" VARCHAR(255),
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    main_pictures VARCHAR(255)[] NOT NULL,
    picture VARCHAR(255) NOT NULL,
    detail TEXT NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_op_order_no ON t_order_products(order_no);
CREATE INDEX IF NOT EXISTS idx_op_product_id ON t_order_products(product_id);

-- 11. Order Product SKUs
CREATE TABLE IF NOT EXISTS t_order_product_skus (
    order_no VARCHAR(100) NOT NULL,
    sku_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    barcode VARCHAR(255) NOT NULL,
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    cost_price NUMERIC(8,2) NOT NULL,
    profit_money NUMERIC(8,2) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    picture VARCHAR(255) NOT NULL,
    specs JSONB NOT NULL DEFAULT '[]'::jsonb,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_opskus_order_no ON t_order_product_skus(order_no);
CREATE INDEX IF NOT EXISTS idx_opskus_product_id ON t_order_product_skus(product_id);
CREATE INDEX IF NOT EXISTS idx_opskus_sku_id ON t_order_product_skus(sku_id);
CREATE INDEX IF NOT EXISTS idx_ops_specs_spec_id ON t_order_product_skus USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_ops_specs_value_id ON t_order_product_skus USING GIN ((specs->'value_id'));

-- 12. Order Product Properties
CREATE TABLE IF NOT EXISTS t_order_product_properties (
    order_no VARCHAR(100) NOT NULL,
    property_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    value_name VARCHAR(255),
    value_id CHAR(36),
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_opp_order_no ON t_order_product_properties(order_no);
CREATE INDEX IF NOT EXISTS idx_opp_property_id ON t_order_product_properties(property_id);
CREATE INDEX IF NOT EXISTS idx_opp_product_id ON t_order_product_properties(product_id);
CREATE INDEX IF NOT EXISTS idx_opp_value_id ON t_order_product_properties(value_id);

-- 13. Order Receiver
CREATE TABLE IF NOT EXISTS t_order_receiver (
    order_no CHAR(100) NOT NULL,
    receiver VARCHAR(100) NOT NULL,
    contact VARCHAR(100) NOT NULL,
    province_code VARCHAR(100) NOT NULL,
    city_code VARCHAR(100) NOT NULL,
    county_code VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_or_order_no ON t_order_receiver(order_no);

-- 14. Member
CREATE TABLE IF NOT EXISTS t_member (
    id CHAR(36) PRIMARY KEY,
    openid VARCHAR(100),
    account VARCHAR(100) NOT NULL,
    mobile VARCHAR(100) NOT NULL,
    avatar VARCHAR(255) NOT NULL DEFAULT '',
    nickname VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_member_mobile ON t_member(mobile);
CREATE INDEX IF NOT EXISTS idx_member_openid ON t_member(openid);

-- 15. Receiver Address
CREATE TABLE IF NOT EXISTS t_receiver (
    id CHAR(36) PRIMARY KEY,
    member_id CHAR(36) NOT NULL,
    receiver VARCHAR(100) NOT NULL,
    contact VARCHAR(100) NOT NULL,
    province_code VARCHAR(100) NOT NULL,
    city_code VARCHAR(100) NOT NULL,
    county_code VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_default SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_receiver_member_id ON t_receiver(member_id);

-- 16. Cart
CREATE TABLE IF NOT EXISTS t_cart (
    id CHAR(36) PRIMARY KEY,
    member_id CHAR(36) NOT NULL,
    sku_id CHAR(36) NOT NULL,
    product_id CHAR(36),
    name VARCHAR(255) NOT NULL,
    specs JSONB NOT NULL DEFAULT '[]'::jsonb,
    count INT NOT NULL DEFAULT 1,
    picture VARCHAR(255) NOT NULL,
    selected SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cart_member_id ON t_cart(member_id);
CREATE INDEX IF NOT EXISTS idx_cart_sku_id ON t_cart(sku_id);
CREATE INDEX IF NOT EXISTS idx_cart_specs_spec_id ON t_cart USING GIN ((specs->'spec_id'));
CREATE INDEX IF NOT EXISTS idx_cart_specs_value_id ON t_cart USING GIN ((specs->'value_id'));

-- 17. Shop Config
CREATE TABLE IF NOT EXISTS t_shop (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    logo VARCHAR(255) NOT NULL,
    free_shipping_amount NUMERIC(8,2) NOT NULL DEFAULT 20.00,
    banners JSON NOT NULL DEFAULT '[]'::json,
    contact VARCHAR(100),
    detail TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 18. Admin
CREATE TABLE IF NOT EXISTS t_admin (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 19. Hot Products
CREATE TABLE IF NOT EXISTS t_hot_products (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_hot_product_id ON t_hot_products(product_id);

-- Seed: default admin
INSERT INTO t_admin (id, account, password, create_time)
VALUES (gen_random_uuid(), 'admin', 'jAQ6D/jGCO8rVN1T6gnhhQ==:pIqizl5zIE68l0w8Dr0XKuGsIZz9t7KZ5u5B+XDseeA=', now()::timestamp(0))
ON CONFLICT DO NOTHING;

-- 20. Admin Login Error Log (shop admin risk control, tenant-scoped)
CREATE TABLE IF NOT EXISTS c_admin_login_error_log (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    login_ip VARCHAR(100) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alert_account ON c_admin_login_error_log(account);
CREATE INDEX IF NOT EXISTS idx_alert_time ON c_admin_login_error_log(create_time);

-- 21. Admin Login Lock (shop admin risk control, tenant-scoped)
CREATE TABLE IF NOT EXISTS c_admin_login_lock (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    lock_end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_allock_account ON c_admin_login_lock(account);

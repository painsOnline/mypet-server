-- =====================================================
-- File: init-empty.sql
-- Author: system
-- Date: 2026-05-03
-- Description: Initialize mypet_empty template database
-- =====================================================

-- Create empty template database (run manually as superuser)
-- CREATE DATABASE mypet_empty;

-- 1. Product Category
CREATE TABLE IF NOT EXISTS t_product_category (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    picture VARCHAR(255),
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 2. Product Type
CREATE TABLE IF NOT EXISTS t_product_type (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 3. Product Specs (defined per type)
CREATE TABLE IF NOT EXISTS t_product_specs (
    id CHAR(36) PRIMARY KEY,
    product_type CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type SMALLINT NOT NULL DEFAULT 1,
    input_type SMALLINT NOT NULL DEFAULT 1,
    input_options VARCHAR(255)[] NOT NULL DEFAULT '{}',
    "desc" VARCHAR(255),
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_specs_type ON t_product_specs(product_type);

-- 4. Product
CREATE TABLE IF NOT EXISTS t_product (
    id CHAR(36) PRIMARY KEY,
    product_type CHAR(36) NOT NULL,
    product_category CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    "desc" VARCHAR(255),
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    main_pictures VARCHAR(255)[] NOT NULL DEFAULT '{}',
    picture VARCHAR(255) NOT NULL,
    detail TEXT NOT NULL DEFAULT '',
    sort INT NOT NULL DEFAULT 0,
    is_enable SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_product_type ON t_product(product_type);
CREATE INDEX IF NOT EXISTS idx_product_category ON t_product(product_category);

-- 5. Product Properties (display params)
CREATE TABLE IF NOT EXISTS t_product_properties (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    product_type CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    value_name VARCHAR(255) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_prop_product ON t_product_properties(product_id);

-- 6. Product SKU
CREATE TABLE IF NOT EXISTS t_product_sku (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    product_type CHAR(36) NOT NULL,
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    inventory INT NOT NULL DEFAULT 0,
    picture VARCHAR(255) NOT NULL DEFAULT '',
    specs JSON NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sku_product ON t_product_sku(product_id);

-- 7. Order
CREATE TABLE IF NOT EXISTS t_order (
    id CHAR(36) PRIMARY KEY,
    order_type SMALLINT NOT NULL DEFAULT 0,
    order_status SMALLINT NOT NULL DEFAULT 1,
    product_type CHAR(36) NOT NULL,
    total_money NUMERIC(8,2) NOT NULL,
    actual_pay_money NUMERIC(8,2) NOT NULL,
    pay_money NUMERIC(8,2) NOT NULL,
    delivery_time VARCHAR(255),
    pay_channel SMALLINT NOT NULL DEFAULT 1,
    pay_type SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_order_status ON t_order(order_status);

-- 8. Order Products (snapshot)
CREATE TABLE IF NOT EXISTS t_order_products (
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    product_type CHAR(36) NOT NULL,
    product_category CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    "desc" VARCHAR(255),
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    main_pictures VARCHAR(255)[] NOT NULL DEFAULT '{}',
    picture VARCHAR(255) NOT NULL,
    detail TEXT NOT NULL DEFAULT '',
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP,
    PRIMARY KEY (order_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_op_order ON t_order_products(order_id);

-- 9. Order Product SKUs (snapshot)
CREATE TABLE IF NOT EXISTS t_order_product_skus (
    order_id CHAR(36) NOT NULL,
    sku_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    product_type CHAR(36) NOT NULL,
    price NUMERIC(8,2) NOT NULL,
    old_price NUMERIC(8,2) NOT NULL,
    inventory INT NOT NULL DEFAULT 0,
    picture VARCHAR(255) NOT NULL DEFAULT '',
    specs JSON NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP,
    PRIMARY KEY (order_id, sku_id)
);
CREATE INDEX IF NOT EXISTS idx_ops_order ON t_order_product_skus(order_id);

-- 10. Member (mini-program user)
CREATE TABLE IF NOT EXISTS t_member (
    id CHAR(36) PRIMARY KEY,
    account VARCHAR(100) NOT NULL,
    mobile VARCHAR(100) NOT NULL,
    avatar VARCHAR(255) NOT NULL DEFAULT '',
    nickname VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_member_mobile ON t_member(mobile);

-- 11. Receiver Address
CREATE TABLE IF NOT EXISTS t_receiver (
    id CHAR(36) PRIMARY KEY,
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

-- 12. Cart
CREATE TABLE IF NOT EXISTS t_cart (
    id CHAR(36) PRIMARY KEY,
    sku_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    specs JSON NOT NULL DEFAULT '{}',
    count INT NOT NULL DEFAULT 1,
    price NUMERIC(8,2) NOT NULL,
    now_price NUMERIC(8,2) NOT NULL,
    picture VARCHAR(255) NOT NULL DEFAULT '',
    selected SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 13. Shop Config (single row, no id)
CREATE TABLE IF NOT EXISTS t_shop (
    name VARCHAR(255) NOT NULL DEFAULT '',
    logo VARCHAR(255) NOT NULL DEFAULT '',
    free_shipping_amount NUMERIC(8,2) NOT NULL DEFAULT 20.00,
    banners VARCHAR(255)[] DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 14. Admin User (no id, single or few rows)
CREATE TABLE IF NOT EXISTS t_admin (
    account VARCHAR(100) NOT NULL,
    "password" VARCHAR(100) NOT NULL,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 15. Hot Products (no id)
CREATE TABLE IF NOT EXISTS t_hot_products (
    product_id CHAR(36) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- 16. Order Receiver (snapshot at order time)
CREATE TABLE IF NOT EXISTS t_order_receiver (
    order_id CHAR(36) PRIMARY KEY,
    receiver VARCHAR(100) NOT NULL,
    contact VARCHAR(100) NOT NULL,
    province_code VARCHAR(100) NOT NULL,
    city_code VARCHAR(100) NOT NULL,
    county_code VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

-- Seed data: default admin (password: admin123, SHA-256+salt)
INSERT INTO t_admin (account, "password", create_time)
VALUES ('admin', 'COmNLQFQqVVxIgF6DKp0Tg==:2WHomXGQ8r9UCgVb8gRn0UEsf0T7lN286IBdfybHGvw=', now()::timestamp(0));

-- Seed data: default shop config
INSERT INTO t_shop (name, logo, free_shipping_amount, banners, create_time)
VALUES ('宠物用品社区店', '', 20.00, '{}', now()::timestamp(0));

-- =====================================================
-- Migration: 013_add_llm_and_import_tables
-- Target: mypet_empty + all tenant databases (mypet_{tenantCode})
-- Description: Add 4 new tables — external product import log,
--              chat messages, agent action logs, shop LLM config.
--              All idempotent (IF NOT EXISTS).
-- =====================================================

-- ============================================================
-- 013-1: 第三方导入商品记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_ext_product_import_log (
    id uuid PRIMARY KEY,
    ext_from VARCHAR(20) NOT NULL CHECK (ext_from IN ('1688', 'taobao')),
    ext_product_id VARCHAR(100) NOT NULL,
    ext_product_name VARCHAR(500) NOT NULL,
    main_picture VARCHAR(500) NOT NULL,
    pictures VARCHAR(500)[] NOT NULL,
    detail_pictures VARCHAR(500)[],
    attrs JSONB,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_eil_ext_from ON t_ext_product_import_log(ext_from);
CREATE INDEX IF NOT EXISTS idx_eil_ext_product_id ON t_ext_product_import_log(ext_product_id);
CREATE INDEX IF NOT EXISTS idx_eil_ext_product_name ON t_ext_product_import_log(ext_product_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_eil_from_pid ON t_ext_product_import_log(ext_from, ext_product_id);

-- ============================================================
-- 013-2: LLM会话记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_chat_messages (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    import_product_id uuid NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system', 'tool')),
    content JSONB NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cm_user_id ON t_chat_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_cm_import_product_id ON t_chat_messages(import_product_id);
CREATE INDEX IF NOT EXISTS idx_cm_role ON t_chat_messages(role);
CREATE INDEX IF NOT EXISTS idx_cm_content ON t_chat_messages USING GIN (content);
CREATE INDEX IF NOT EXISTS idx_cm_user_prod_time ON t_chat_messages(user_id, import_product_id, create_time);

-- ============================================================
-- 013-3: LLM 操作日志
-- ============================================================
CREATE TABLE IF NOT EXISTS t_agent_action_logs (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    import_product_id uuid NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('success', 'partial', 'failure')),
    request JSONB NOT NULL,
    response JSONB NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_aal_user_id ON t_agent_action_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_aal_import_product_id ON t_agent_action_logs(import_product_id);
CREATE INDEX IF NOT EXISTS idx_aal_action_type ON t_agent_action_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_aal_status ON t_agent_action_logs(status);
CREATE INDEX IF NOT EXISTS idx_aal_user_prod_type ON t_agent_action_logs(user_id, import_product_id, action_type);

-- ============================================================
-- 013-4: 店铺大模型配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_shop_llm_config (
    id uuid PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,
    api_key VARCHAR(100) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    temperature NUMERIC(3,2) NOT NULL DEFAULT 0.3,
    max_tokens INTEGER NOT NULL DEFAULT 4096,
    timeout_seconds INTEGER NOT NULL DEFAULT 60,
    max_retries INTEGER NOT NULL DEFAULT 3,
    create_time TIMESTAMP NOT NULL DEFAULT now()::timestamp(0),
    modify_time TIMESTAMP
);

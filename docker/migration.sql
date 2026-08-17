-- 订单表重构：从「单商品订单」改为「主订单 + 订单明细」模式
-- 执行前请备份数据

-- 1. 重建 orders 表（去掉商品字段，新增 item_count）
DROP TABLE IF EXISTS orders_new;
CREATE TABLE orders_new (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    item_count INT NOT NULL DEFAULT 1 COMMENT '商品种类数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 2. 迁移旧数据（如果有）
INSERT INTO orders_new (order_no, user_id, total_amount, item_count, status, deleted, created_at, updated_at)
SELECT order_no, user_id, total_amount, 1, status, deleted, created_at, updated_at FROM orders;

-- 3. 替换表
DROP TABLE orders;
RENAME TABLE orders_new TO orders;

-- 4. order_items 表加 created_at
ALTER TABLE order_items ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP AFTER item_amount;

-- 5. 验证
SELECT 'Migration complete. orders table restructured.' AS result;

-- ========= 2026-08-17 支付功能 =========
-- orders 表增加支付时间字段（存量库执行）
ALTER TABLE orders ADD COLUMN paid_at DATETIME DEFAULT NULL COMMENT '支付时间' AFTER status;

-- 验证
DESC orders;

-- 支付流水表（支付宝沙箱接入）
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_no VARCHAR(64) NOT NULL UNIQUE COMMENT '支付单号（商户侧，传给支付宝的 out_trade_no）',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '商户订单号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号（支付成功后回填）',
    channel VARCHAR(20) NOT NULL COMMENT '支付渠道: ALIPAY/MOCK',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PAYING' COMMENT '状态: PAYING/SUCCESS/FAILED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

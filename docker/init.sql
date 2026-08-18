-- 设置客户端字符集，确保中文正确读入
SET NAMES utf8mb4;

-- Cloud Mall Lab - Database Init（三库三账号版）
-- 全新环境由 docker-entrypoint-initdb.d 以 root 自动执行一次成型
-- 存量库升级不走本文件：增量 SQL 由对话下发、人工执行

-- ===== 1. 建库（服务独立库，跨服务一致性由 Seata 保证）=====
CREATE DATABASE IF NOT EXISTS cloud_user     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cloud_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cloud_order   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ===== 2. 业务账号（账号=库名，最小权限：只授自己的库）=====
CREATE USER IF NOT EXISTS 'cloud_user'     @'%' IDENTIFIED BY 'cloud123';
CREATE USER IF NOT EXISTS 'cloud_product' @'%' IDENTIFIED BY 'cloud123';
CREATE USER IF NOT EXISTS 'cloud_order'   @'%' IDENTIFIED BY 'cloud123';
GRANT ALL PRIVILEGES ON cloud_user.*     TO 'cloud_user'@'%';
GRANT ALL PRIVILEGES ON cloud_product.* TO 'cloud_product'@'%';
GRANT ALL PRIVILEGES ON cloud_order.*   TO 'cloud_order'@'%';

-- ===== 3. 观察账号（只读三库，人工查数用）=====
CREATE USER IF NOT EXISTS 'cloudlab' @'%' IDENTIFIED BY 'cloud123';
GRANT SELECT ON cloud_user.*     TO 'cloudlab'@'%';
GRANT SELECT ON cloud_product.* TO 'cloudlab'@'%';
GRANT SELECT ON cloud_order.*   TO 'cloudlab'@'%';
FLUSH PRIVILEGES;

-- ===================== cloud_user 库（lab-user） =====================
USE cloud_user;

DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS undo_log;

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入测试数据（密码均为 123456 的BCrypt加密）
INSERT INTO users (username, password, email, phone, status, role) VALUES
('admin', '$2a$10$3jRFz.xlWGU83GgHkwz32u3I6XyEm1pz5iz9RXba.eg/hsJxHfgo6', 'admin@cloudlab.com', '13800138000', 1, 'admin'),
('test01', '$2a$10$OeGu8T.VrL7i2xl9m1/ENOSouyJ95J2DNZeWY3KbB2QdePr8T0mzm', 'test01@cloudlab.com', '13800138001', 1, 'user'),
('test02', '$2a$10$OtOqMbUDjLV9Blq9x2XwyuzkBwwJZQzJkM2hzLsVxvWQ8SMX8A5Dy', 'test02@cloudlab.com', '13800138002', 1, 'user');

-- Seata AT模式回滚日志表（参与全局事务的业务库各一张）
CREATE TABLE undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文（序列化方式等）',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚镜像（修改前后的数据快照）',
    log_status    INT          NOT NULL COMMENT '状态：0正常 1全局已完成（防回滚竞态）',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式回滚日志表';

-- ===================== cloud_product 库（lab-product） =====================
USE cloud_product;

DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS undo_log;

-- 商品表
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description VARCHAR(1000) COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    category VARCHAR(50) COMMENT '分类',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 商品测试数据
INSERT INTO products (name, description, price, stock, category, status) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB', 8999.00, 100, '手机', 1),
('MacBook Pro 14', 'Apple MacBook Pro 14英寸 M3 Pro', 16999.00, 50, '电脑', 1),
('AirPods Pro 2', 'Apple AirPods Pro 第二代', 1799.00, 200, '配件', 1),
('iPad Air', 'Apple iPad Air M2 256GB', 5499.00, 80, '平板', 1),
('Apple Watch S9', 'Apple Watch Series 9 GPS', 2999.00, 120, '手表', 1);

-- Seata AT模式回滚日志表（参与全局事务的业务库各一张）
CREATE TABLE undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文（序列化方式等）',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚镜像（修改前后的数据快照）',
    log_status    INT          NOT NULL COMMENT '状态：0正常 1全局已完成（防回滚竞态）',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式回滚日志表';

-- ===================== cloud_order 库（lab-order） =====================
USE cloud_order;

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS undo_log;

-- 订单主表（不含商品字段，商品信息在 order_items）
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    item_count INT NOT NULL DEFAULT 1 COMMENT '商品种类数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/SHIPPED/COMPLETED/CANCELLED',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    stock_restored TINYINT NOT NULL DEFAULT 0 COMMENT '取消单库存回补: 0=未回补(悬挂账,等对账) 1=已回补; 非取消单恒0',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 购物车表
CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 订单明细表
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '主订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_price DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
    quantity INT NOT NULL COMMENT '数量',
    item_amount DECIMAL(10,2) NOT NULL COMMENT '小计',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 支付流水表
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_no VARCHAR(64) NOT NULL UNIQUE COMMENT '支付单号（商户侧，传给支付宝的 out_trade_no）',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '商户订单号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号（支付成功后回填）',
    channel VARCHAR(20) NOT NULL COMMENT '支付渠道: ALIPAY/MOCK',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PAYING' COMMENT '状态: PAYING/SUCCESS/FAILED/CLOSED(超时关单)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- Seata AT模式回滚日志表（参与全局事务的业务库各一张）
-- Seata 数据源代理自动写入前后镜像，全局回滚时据此逆向补偿，提交后自动清理
CREATE TABLE undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文（序列化方式等）',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚镜像（修改前后的数据快照）',
    log_status    INT          NOT NULL COMMENT '状态：0正常 1全局已完成（防回滚竞态）',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式回滚日志表';


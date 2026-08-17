-- 设置客户端字符集，确保中文正确读入
SET NAMES utf8mb4;

-- Cloud Mall Lab - Database Init

-- 先删除旧表（开发阶段使用）
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS products;

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
('test01', '$2a$10$OeGu8T.VrL7i2xl9m1/ENOSouyJ95J2DNZeWY3KbB2QdePr8t0mzm', 'test01@cloudlab.com', '13800138001', 1, 'user'),
('test02', '$2a$10$OtOqMbUDjLV9Blq9x2XwyuzkBwwJZQzJkM2hzLsVxvWQ8SMX8A5Dy', 'test02@cloudlab.com', '13800138002', 1, 'user');

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

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    quantity INT NOT NULL COMMENT '数量',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- Cloud Mall Lab - Database Init

-- 先删除旧表（开发阶段使用）
DROP TABLE IF EXISTS users;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试数据（密码均为 123456 的BCrypt加密）
INSERT INTO users (username, password, email, phone, status, role) VALUES
('admin', '$2a$10$3jRFz.xlWGU83GgHkwz32u3I6XyEm1pz5iz9RXba.eg/hsJxHfgo6', 'admin@cloudlab.com', '13800138000', 1, 'admin'),
('test01', '$2a$10$OeGu8T.VrL7i2xl9m1/ENOSouyJ95J2DNZeWY3KbB2QdePr8t0mzm', 'test01@cloudlab.com', '13800138001', 1, 'user'),
('test02', '$2a$10$OtOqMbUDjLV9Blq9x2XwyuzkBwwJZQzJkM2hzLsVxvWQ8SMX8A5Dy', 'test02@cloudlab.com', '13800138002', 1, 'user');

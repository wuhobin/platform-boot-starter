-- MySQL 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS demo DEFAULT CHARACTER SET utf8mb4;

USE demo;

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    age         INT,
    email       VARCHAR(128),
    status      TINYINT      DEFAULT 0,
    version     INT          DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 测试数据
INSERT INTO t_user (name, age, email, status) VALUES
('张三', 25, 'zhangsan@example.com', 0),
('李四', 30, 'lisi@example.com', 1),
('王五', 28, 'wangwu@example.com', 0);
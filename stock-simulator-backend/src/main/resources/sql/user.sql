-- 用户表（注册模块）
CREATE DATABASE IF NOT EXISTS stock_simulator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE stock_simulator;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    user_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(32)  NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码（明文存储，后续可改为加密）',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

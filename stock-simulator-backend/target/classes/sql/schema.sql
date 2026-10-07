CREATE DATABASE IF NOT EXISTS stock_simulator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE stock_simulator;

-- 用户表
DROP TABLE IF EXISTS trade_record;
DROP TABLE IF EXISTS user_hold_stock;
DROP TABLE IF EXISTS user_stock_price;
DROP TABLE IF EXISTS userinfo;
DROP TABLE IF EXISTS news;
DROP TABLE IF EXISTS stock_info;
DROP TABLE IF EXISTS user;

CREATE TABLE `user` (
    user_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(32)  NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE userinfo (
    user_id   BIGINT         NOT NULL COMMENT '用户ID，对应 user.user_id',
    username  VARCHAR(32)    NOT NULL COMMENT '用户名',
    money     DECIMAL(18, 2) NOT NULL COMMENT '游戏内金钱',
    game_time VARCHAR(32)    NOT NULL COMMENT '游戏内时间',
    game_date VARCHAR(32)    NOT NULL COMMENT '游戏内日期',
    difficulty INT           NOT NULL DEFAULT 1 COMMENT '游戏难度：0简单 1中等 2困难',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_userinfo_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户游戏信息';

CREATE TABLE user_stock_price (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT         NOT NULL COMMENT '用户ID，关联 user.user_id',
    stock_name       VARCHAR(64)    NOT NULL COMMENT '股票名称',
    stock_code       VARCHAR(16)    NOT NULL COMMENT '股票代码',
    open_price       DECIMAL(18, 2) NOT NULL COMMENT '当日开盘价',
    last_trade_price DECIMAL(18, 2) NOT NULL COMMENT '上个交易日收盘价',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stock_code (user_id, stock_code),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户存档下股票开盘价与昨收价';

CREATE TABLE user_hold_stock (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    stock_code  VARCHAR(32)  NOT NULL COMMENT '股票代码',
    stock_name  VARCHAR(64)  NOT NULL COMMENT '股票名称',
    quantity    INT          NOT NULL DEFAULT 0 COMMENT '持有数量（股）',
    avg_price   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '持仓成本均价',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_stock (user_id, stock_code),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持仓';

CREATE TABLE trade_record (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    stock_code  VARCHAR(32)  NOT NULL,
    stock_name  VARCHAR(64)  NOT NULL,
    trade_type  TINYINT      NOT NULL COMMENT '1买入 2卖出',
    price       DECIMAL(18, 4) NOT NULL COMMENT '成交单价（前端模拟价）',
    quantity    INT          NOT NULL COMMENT '成交数量',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '成交金额',
    trade_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_trade_time (trade_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录';

CREATE TABLE stock_info (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code     VARCHAR(32)  NOT NULL UNIQUE COMMENT '股票代码',
    stock_name     VARCHAR(64)  NOT NULL COMMENT '股票名称',
    industry       VARCHAR(64)  NOT NULL COMMENT '行业分类',
    history_prices JSON         NOT NULL COMMENT '历史价格点数组，供折线图',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票基础信息';

CREATE TABLE news (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(256) NOT NULL,
    content      TEXT         NOT NULL,
    publish_time DATETIME     NOT NULL COMMENT '发布时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻';

USE stock_simulator;

DROP TABLE IF EXISTS user_stock_price;

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

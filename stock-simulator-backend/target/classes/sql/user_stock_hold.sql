USE stock_simulator;

DROP TABLE IF EXISTS user_stock_hold;

CREATE TABLE user_stock_hold (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '用户ID，关联 user.user_id',
    stock_code  VARCHAR(16) NOT NULL COMMENT '股票代码',
    hold_count      INT            NOT NULL DEFAULT 0 COMMENT '持股股数，不能为负数',
    buy_total_cost  DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '该只股票总买入成本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stock_code (user_id, stock_code),
    KEY idx_user_id (user_id),
    CONSTRAINT chk_hold_count_non_negative CHECK (hold_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户持仓（股数）';

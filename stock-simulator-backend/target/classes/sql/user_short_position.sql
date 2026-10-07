USE stock_simulator;

CREATE TABLE IF NOT EXISTS user_short_position (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT         NOT NULL COMMENT '用户ID',
    stock_code       VARCHAR(16)    NOT NULL COMMENT '股票代码',
    short_count      INT            NOT NULL DEFAULT 0 COMMENT '借入总股数（融券负债）',
    short_sold_count INT            NOT NULL DEFAULT 0 COMMENT '已卖出股数',
    not_sold_count   INT            NOT NULL DEFAULT 0 COMMENT '借入尚未卖出股数',
    borrow_price       DECIMAL(18, 2) NOT NULL COMMENT '借券基准价（加权）',
    borrow_base_total  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借券基准总金额',
    create_game_date   VARCHAR(32)    NOT NULL COMMENT '借券游戏日期',
    create_game_time VARCHAR(32)    NOT NULL COMMENT '借券游戏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_short_stock (user_id, stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户融券做空负债';

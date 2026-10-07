USE stock_simulator;

CREATE TABLE IF NOT EXISTS stock_event (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT         NOT NULL COMMENT '用户ID',
    stock_code    VARCHAR(16)    NOT NULL COMMENT '受影响股票代码',
    event_title   VARCHAR(128)   NOT NULL COMMENT '事件标题',
    event_content VARCHAR(512)   NOT NULL COMMENT '事件完整描述',
    change_rate   DECIMAL(8, 4)  NOT NULL COMMENT '事件涨跌幅，如 -0.1800 表示 -18%',
    game_date     VARCHAR(32)    NOT NULL COMMENT '游戏内日期',
    game_time     VARCHAR(32)    NOT NULL COMMENT '游戏内时间',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (id),
    KEY idx_stock_event_user_time (user_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户随机事件记录';

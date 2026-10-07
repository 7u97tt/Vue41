USE stock_simulator;

CREATE TABLE IF NOT EXISTS user_operate_log (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT         NOT NULL COMMENT '用户ID',
    operate_type VARCHAR(64)    NOT NULL COMMENT '操作类型：买入股票、卖出股票、手续费扣费等',
    stock_code   VARCHAR(16)    NULL COMMENT '股票代码，可为空',
    change_money   DECIMAL(18, 2) NOT NULL COMMENT '变动金额绝对值（流入/流出为正数，无变动为0）',
    fund_direction TINYINT         NOT NULL DEFAULT 0 COMMENT '1流入 -1流出 0无资金变动',
    after_money    DECIMAL(18, 2) NOT NULL COMMENT '操作后可用金钱',
    game_date    VARCHAR(32)    NOT NULL COMMENT '游戏内日期',
    game_time    VARCHAR(32)    NOT NULL COMMENT '游戏内时间',
    remark       VARCHAR(256)   NULL COMMENT '操作备注',
    create_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (id),
    KEY idx_operate_log_user_time (user_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户操作日志';

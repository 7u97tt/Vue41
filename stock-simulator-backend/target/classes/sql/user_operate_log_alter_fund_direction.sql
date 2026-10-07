USE stock_simulator;

ALTER TABLE user_operate_log
    ADD COLUMN fund_direction TINYINT NOT NULL DEFAULT 0 COMMENT '1流入 -1流出 0无资金变动' AFTER change_money;

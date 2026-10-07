USE stock_simulator;

ALTER TABLE user_stock_price
    ADD COLUMN open_price DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '当日开盘价'
    AFTER stock_code;

-- 已有数据：首日开盘价与昨收（last_trade_price）对齐
UPDATE user_stock_price SET open_price = last_trade_price WHERE open_price = 0;

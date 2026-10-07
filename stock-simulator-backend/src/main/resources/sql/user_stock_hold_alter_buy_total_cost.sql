USE stock_simulator;

ALTER TABLE user_stock_hold
    ADD COLUMN buy_total_cost DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '该只股票总买入成本' AFTER hold_count;

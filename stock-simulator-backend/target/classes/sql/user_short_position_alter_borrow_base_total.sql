USE stock_simulator;

ALTER TABLE user_short_position
    ADD COLUMN borrow_base_total DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借券基准总金额 borrowCount×借出时股价' AFTER borrow_price;

UPDATE user_short_position
SET borrow_base_total = ROUND(short_count * borrow_price, 2)
WHERE borrow_base_total = 0 AND short_count > 0;

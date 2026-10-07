USE stock_simulator;

CREATE TABLE IF NOT EXISTS broker_stock (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    stock_code       VARCHAR(16) NOT NULL COMMENT '股票代码',
    can_borrow_count INT         NOT NULL COMMENT '券商可借库存',
    PRIMARY KEY (id),
    UNIQUE KEY uk_broker_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='券商融券库存';

INSERT INTO broker_stock (stock_code, can_borrow_count) VALUES
('601001', 10000), ('601002', 10000), ('602001', 10000), ('603001', 10000), ('603002', 10000),
('604001', 10000), ('604002', 10000), ('605001', 10000), ('605002', 10000), ('605003', 10000),
('605004', 10000), ('606001', 10000), ('606002', 10000), ('607001', 10000), ('607002', 10000),
('608001', 10000), ('608002', 10000), ('608003', 10000), ('609001', 10000), ('609002', 10000),
('610001', 10000), ('610002', 10000), ('610003', 10000), ('611001', 10000), ('611002', 10000),
('612001', 10000), ('612002', 10000), ('613001', 10000), ('613002', 10000), ('613003', 10000)
ON DUPLICATE KEY UPDATE can_borrow_count = VALUES(can_borrow_count);

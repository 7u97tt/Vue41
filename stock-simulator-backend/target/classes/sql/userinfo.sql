USE stock_simulator;

DROP TABLE IF EXISTS userinfo;

CREATE TABLE userinfo (
    user_id   BIGINT         NOT NULL COMMENT '用户ID，对应 user.user_id',
    username  VARCHAR(32)    NOT NULL COMMENT '用户名',
    money     DECIMAL(18, 2) NOT NULL COMMENT '游戏内金钱',
    game_time VARCHAR(32)    NOT NULL COMMENT '游戏内时间，如 13:35',
    game_date VARCHAR(32)    NOT NULL COMMENT '游戏内日期',
    difficulty INT           NOT NULL DEFAULT 1 COMMENT '游戏难度：0简单 1中等 2困难',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_userinfo_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户游戏信息';

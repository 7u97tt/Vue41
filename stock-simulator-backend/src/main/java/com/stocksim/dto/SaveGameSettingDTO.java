package com.stocksim.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 保存游戏难度与初始设置。
 * <p>Redis 使用 {@code userId} 拼接 key，例如 {@code "1Difficulty"}、hash {@code "1info"}，不再使用 username。</p>
 */
@Data
public class SaveGameSettingDTO {

    /** 必填，用于 Redis key 与 MySQL userinfo.user_id */
    private Long userId;
    /** 用于校验与写入 userinfo.username，不参与 Redis key */
    private String username;
    /** 简单 / 中等 / 困难 / 自定义（落库为 int；Redis 存 0/1/2/custom） */
    private String difficulty;
    private BigDecimal money;
    private String gameTime;
    private String gameDate;
}

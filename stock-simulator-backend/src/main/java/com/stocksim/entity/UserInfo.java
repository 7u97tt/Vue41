package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserInfo {

    private Long userId;
    private String username;
    private BigDecimal money;
    private String gameTime;
    private String gameDate;
    /** 0简单 1中等 2困难 */
    private Integer difficulty;
}

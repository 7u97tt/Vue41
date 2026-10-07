package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserHoldStock {

    private Long id;
    private Long userId;
    private String stockCode;
    private String stockName;
    private Integer quantity;
    private BigDecimal avgPrice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

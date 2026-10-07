package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeRecord {

    public static final int TYPE_BUY = 1;
    public static final int TYPE_SELL = 2;

    private Long id;
    private Long userId;
    private String stockCode;
    private String stockName;
    private Integer tradeType;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private LocalDateTime tradeTime;
}

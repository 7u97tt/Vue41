package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockEvent {

    private Long id;
    private Long userId;
    private String stockCode;
    private String eventTitle;
    private String eventContent;
    private BigDecimal changeRate;
    private String gameDate;
    private String gameTime;
    private LocalDateTime createTime;
}

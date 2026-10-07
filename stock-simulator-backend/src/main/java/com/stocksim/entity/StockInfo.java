package com.stocksim.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockInfo {

    private Long id;
    private String stockCode;
    private String stockName;
    private String industry;
    /** JSON 字符串，如 [10.1,10.2,...] */
    private String historyPrices;
    private LocalDateTime createTime;
}

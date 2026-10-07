package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserStockPrice {

    private Long id;
    private Long userId;
    private String stockName;
    private String stockCode;
    /** 当日开盘价 */
    private BigDecimal openPrice;
    /** 上个交易日收盘价 */
    private BigDecimal lastTradePrice;
}

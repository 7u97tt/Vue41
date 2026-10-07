package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

/** queryAndUpdateStockPrice 触发随机事件时返回的事件详情 */
@Data
public class StockEventInfoVO {

    private String eventTitle;
    private String eventContent;
    private String stockCode;
    private String stockName;
    private BigDecimal changeRate;
}

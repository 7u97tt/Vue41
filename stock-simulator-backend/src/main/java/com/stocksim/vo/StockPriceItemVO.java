package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockPriceItemVO {

    private String stockCode;
    private String stockName;
    private BigDecimal lastTradePrice;
}

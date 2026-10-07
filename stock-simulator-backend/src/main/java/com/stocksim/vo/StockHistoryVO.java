package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockHistoryVO {

    private String stockCode;
    private String stockName;
    private List<BigDecimal> prices;
}

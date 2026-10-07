package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldStockVO {

    private String stockCode;
    private String stockName;
    private Integer quantity;
    private BigDecimal avgPrice;
    /** 持仓成本市值 */
    private BigDecimal marketValue;
}

package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserStockHold {

    private Long id;
    private Long userId;
    private String stockCode;
    /** 持股股数，不能为负数 */
    private Integer holdCount;
    /** 该只股票总买入成本 */
    private BigDecimal buyTotalCost;
}

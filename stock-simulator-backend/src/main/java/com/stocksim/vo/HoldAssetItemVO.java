package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldAssetItemVO {

    private String stockName;
    private String stockCode;
    private Integer holdCount;
    /** 买入成本价（均价） */
    private BigDecimal buyCostPrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal profitAmount;
    /** 盈亏百分比 */
    private BigDecimal profitRate;
}

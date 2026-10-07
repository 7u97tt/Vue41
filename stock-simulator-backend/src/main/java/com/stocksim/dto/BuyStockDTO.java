package com.stocksim.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BuyStockDTO {

    private Long userId;
    private String stockCode;
    /** 购买股数，必须大于 0 */
    private Integer buyCount;
    /** 买入手续费率，默认 0.003（0.3%） */
    private BigDecimal handFeeRate;
}

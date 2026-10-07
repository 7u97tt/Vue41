package com.stocksim.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellStockVO {

    /** 卖出股数 */
    private Integer sellCount;
    /** 卖出成交总金额 */
    private BigDecimal tradeAmount;
    /** 卖出手续费 */
    private BigDecimal handFee;
    /** 实际到账金额 */
    private BigDecimal netReceived;
}

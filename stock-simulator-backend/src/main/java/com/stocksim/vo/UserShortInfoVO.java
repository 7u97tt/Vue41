package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserShortInfoVO {

    private String stockCode;
    /** 欠券总股数 */
    private Integer shortCount;
    /** 已卖出股数 */
    private Integer shortSoldCount;
    /** 尚未卖出股数 */
    private Integer notSoldCount;
    private BigDecimal borrowPrice;
    /** 借券基准总金额 */
    private BigDecimal borrowBaseTotal;
}

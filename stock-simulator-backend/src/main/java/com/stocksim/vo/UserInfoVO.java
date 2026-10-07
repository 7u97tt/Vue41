package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    /** 可用资金 */
    private BigDecimal availableMoney;
    /** 总资产（可用资金 + 持仓按成本价估值） */
    private BigDecimal totalAsset;
    /** 相对初始资金的涨跌幅，百分比，如 5.25 表示 +5.25% */
    private BigDecimal assetChangePercent;
    private BigDecimal initialCapital;
}

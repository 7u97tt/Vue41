package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MyAssetVO {

    private BigDecimal availableMoney;
    private BigDecimal holdMarketValue;
    private BigDecimal totalAsset;
    private BigDecimal totalBuyCost;
    private BigDecimal totalProfit;
    /** 资产涨跌幅(%)，无持仓或总成本为 0 时为 null */
    private BigDecimal profitRate;
    /** 持仓明细，供前端「我的持仓」表格 */
    private List<HoldAssetItemVO> holdList;
}

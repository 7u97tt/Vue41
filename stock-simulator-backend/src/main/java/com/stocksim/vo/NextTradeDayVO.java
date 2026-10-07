package com.stocksim.vo;

import lombok.Data;

@Data
public class NextTradeDayVO {

    private String gameDate;
    /** 新交易日开盘时间 */
    private String gameTime;
}

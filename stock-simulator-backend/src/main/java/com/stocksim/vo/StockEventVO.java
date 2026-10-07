package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockEventVO {

    private Long id;
    private String stockCode;
    private String eventTitle;
    private String eventContent;
    /** 事件涨跌幅，如 -0.18 表示 -18% */
    private BigDecimal changeRate;
    private String gameDate;
    private String gameTime;
    private String createTime;
}

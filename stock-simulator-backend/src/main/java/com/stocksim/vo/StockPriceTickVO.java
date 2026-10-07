package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockPriceTickVO {

    private String gameTime;
    private String gameDate;
    private Boolean isTradingEnd;
    private List<StockPriceItemVO> stocks;

    /** 本次 tick 是否触发随机事件 */
    private Boolean eventOccur;
    /** 事件标题；未触发为空 */
    private String eventTitle;
    /** 事件正文；未触发为空 */
    private String eventContent;
    /** 受影响股票代码；未触发为空 */
    private String affectedStockCode;
    /** 事件涨跌幅，如 -0.1650 表示 -16.50%；未触发为 null */
    private BigDecimal changeRate;
}

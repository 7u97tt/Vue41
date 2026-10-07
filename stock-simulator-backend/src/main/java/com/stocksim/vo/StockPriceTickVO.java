package com.stocksim.vo;

import lombok.Data;

import java.util.List;

@Data
public class StockPriceTickVO {

    private String gameTime;
    private String gameDate;
    private Boolean isTradingEnd;
    private List<StockPriceItemVO> stocks;
    /** 本次 tick 是否触发随机事件 */
    private Boolean eventOccur;
    /** 本次事件提示文案；未触发为空 */
    private String eventMsg;
    /** 本次事件详情；未触发为 null */
    private StockEventInfoVO eventInfo;
}

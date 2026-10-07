package com.stocksim.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OperateLogVO {

    private Long id;
    private String operateType;
    private String stockCode;
    private BigDecimal changeMoney;
    /** 1 流入，-1 流出，0 无资金变动 */
    private Integer fundDirection;
    private BigDecimal afterMoney;
    private String gameDate;
    private String gameTime;
    private String remark;
    private String createTime;
}

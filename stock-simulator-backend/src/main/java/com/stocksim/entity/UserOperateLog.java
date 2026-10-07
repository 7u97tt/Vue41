package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserOperateLog {

    private Long id;
    private Long userId;
    private String operateType;
    private String stockCode;
    private BigDecimal changeMoney;
    /** 1 流入，-1 流出，0 无资金变动 */
    private Integer fundDirection;
    private BigDecimal afterMoney;
    private String gameDate;
    private String gameTime;
    private String remark;
    private LocalDateTime createTime;
}

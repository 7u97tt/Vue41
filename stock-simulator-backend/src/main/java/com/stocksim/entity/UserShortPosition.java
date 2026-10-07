package com.stocksim.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserShortPosition {

    private Long id;
    private Long userId;
    private String stockCode;
    private Integer shortCount;
    private Integer shortSoldCount;
    private Integer notSoldCount;
    private BigDecimal borrowPrice;
    /** 借券时刻基准总金额，日手续费按此计算，不随现价变动 */
    private BigDecimal borrowBaseTotal;
    private String createGameDate;
    private String createGameTime;
}

package com.stocksim.dto;

import lombok.Data;

@Data
public class SellStockDTO {

    private Long userId;
    private String stockCode;
    /** 卖出股数，必须大于 0 */
    private Integer sellCount;
}

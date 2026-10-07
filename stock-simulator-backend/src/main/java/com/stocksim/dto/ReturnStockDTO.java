package com.stocksim.dto;

import lombok.Data;

@Data
public class ReturnStockDTO {

    private Long userId;
    private String stockCode;
    private Integer returnCount;
}

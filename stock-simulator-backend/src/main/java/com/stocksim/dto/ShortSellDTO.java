package com.stocksim.dto;

import lombok.Data;

@Data
public class ShortSellDTO {

    private Long userId;
    private String stockCode;
    private Integer sellCount;
}

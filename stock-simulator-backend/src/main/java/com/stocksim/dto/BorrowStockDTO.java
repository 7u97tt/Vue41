package com.stocksim.dto;

import lombok.Data;

@Data
public class BorrowStockDTO {

    private Long userId;
    private String stockCode;
    private Integer borrowCount;
}

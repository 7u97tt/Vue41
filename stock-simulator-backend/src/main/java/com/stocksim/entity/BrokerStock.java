package com.stocksim.entity;

import lombok.Data;

@Data
public class BrokerStock {

    private Long id;
    private String stockCode;
    private Integer canBorrowCount;
}

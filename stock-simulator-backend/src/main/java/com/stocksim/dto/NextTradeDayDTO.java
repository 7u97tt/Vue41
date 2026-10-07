package com.stocksim.dto;

import lombok.Data;

@Data
public class NextTradeDayDTO {

    private Long userId;
    /** yyyy-MM-dd */
    private String currentGameDate;
}

package com.stocksim.dto;

import lombok.Data;

@Data
public class QueryAndUpdateStockPriceDTO {

    private Long userId;
    /** HH:mm */
    private String gameTime;
    /** yyyy-MM-dd */
    private String gameDate;
}

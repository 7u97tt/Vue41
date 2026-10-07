package com.stocksim.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    /** 前端模拟的当前成交价 */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0001", message = "价格必须大于 0")
    private BigDecimal price;
}

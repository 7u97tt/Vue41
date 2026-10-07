package com.stocksim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private BigDecimal initialCapital = new BigDecimal("1000000");
}

package com.stocksim;

import com.stocksim.config.DebugDatasourceProbe;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.stocksim.mapper")
public class StockSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(StockSimulatorApplication.class);
        app.addListeners(new DebugDatasourceProbe());
        app.run(args);
    }
}

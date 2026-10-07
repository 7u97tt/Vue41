package com.stocksim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jedis")
public class JedisProperties {

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private int timeout = 3000;
    private Pool pool = new Pool();

    @Data
    public static class Pool {
        private int maxTotal = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
    }
}

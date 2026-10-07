package com.stocksim.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@EnableConfigurationProperties(JedisProperties.class)
public class JedisConfig {

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool(JedisProperties props) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(props.getPool().getMaxTotal());
        poolConfig.setMaxIdle(props.getPool().getMaxIdle());
        poolConfig.setMinIdle(props.getPool().getMinIdle());

        String password = StringUtils.hasText(props.getPassword()) ? props.getPassword() : null;
        return new JedisPool(
                poolConfig,
                props.getHost(),
                props.getPort(),
                props.getTimeout(),
                password,
                props.getDatabase()
        );
    }
}

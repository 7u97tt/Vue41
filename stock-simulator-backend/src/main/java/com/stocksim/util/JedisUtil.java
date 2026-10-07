package com.stocksim.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Jedis 连接工具：从连接池获取 Jedis，执行后自动关闭。
 */
@Component
@RequiredArgsConstructor
public class JedisUtil {

    private final JedisPool jedisPool;

    public void set(String key, String value) {
        execute(jedis -> {
            jedis.set(key, value);
            return null;
        });
    }

    public void hmset(String key, Map<String, String> hash) {
        execute(jedis -> {
            jedis.hset(key, hash);
            return null;
        });
    }

    public void hset(String key, String field, String value) {
        execute(jedis -> {
            jedis.hset(key, field, value);
            return null;
        });
    }

    public String hget(String key, String field) {
        return execute(jedis -> jedis.hget(key, field));
    }

    public void hdel(String key, String field) {
        execute(jedis -> {
            jedis.hdel(key, field);
            return null;
        });
    }

    public void lpush(String key, String value) {
        execute(jedis -> {
            jedis.lpush(key, value);
            return null;
        });
    }

    public java.util.List<String> lrange(String key, long start, long end) {
        return execute(jedis -> jedis.lrange(key, start, end));
    }

    public void ltrim(String key, long start, long end) {
        execute(jedis -> {
            jedis.ltrim(key, start, end);
            return null;
        });
    }

    public Map<String, String> hgetAll(String key) {
        return execute(jedis -> {
            Map<String, String> map = jedis.hgetAll(key);
            return map != null ? map : new HashMap<>();
        });
    }

    public <T> T execute(Function<Jedis, T> callback) {
        try (Jedis jedis = jedisPool.getResource()) {
            return callback.apply(jedis);
        }
    }
}

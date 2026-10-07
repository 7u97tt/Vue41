package com.stocksim.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserShortPosRedisUtil {

    private final JedisUtil jedisUtil;

    public UserShortPosRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public static String shortPosKey(Long userId) {
        return userId + "shortPos";
    }

    public void saveFieldJson(Long userId, String stockCode, String json) {
        jedisUtil.hset(shortPosKey(userId), stockCode, json);
    }

    public void deleteField(Long userId, String stockCode) {
        jedisUtil.hdel(shortPosKey(userId), stockCode);
    }

    public Map<String, String> getAll(Long userId) {
        return jedisUtil.hgetAll(shortPosKey(userId));
    }

    public String getFieldJson(Long userId, String stockCode) {
        return jedisUtil.hget(shortPosKey(userId), stockCode);
    }

    public boolean hasAnyField(Long userId) {
        Map<String, String> all = getAll(userId);
        return all != null && all.values().stream().anyMatch(StringUtils::hasText);
    }
}

package com.stocksim.util;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户随机事件 Redis List：key = {userId}eventList，最多保留 20 条。
 */
@Component
public class UserStockEventRedisUtil {

    private static final int MAX_EVENTS = 20;

    private final JedisUtil jedisUtil;

    public UserStockEventRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public static String eventListKey(Long userId) {
        return userId + "eventList";
    }

    public void pushEventJson(Long userId, String eventJson) {
        String key = eventListKey(userId);
        jedisUtil.lpush(key, eventJson);
        jedisUtil.ltrim(key, 0, MAX_EVENTS - 1);
    }

    public List<String> listRecentEventJson(Long userId) {
        List<String> list = jedisUtil.lrange(eventListKey(userId), 0, MAX_EVENTS - 1);
        return list != null ? list : Collections.emptyList();
    }
}

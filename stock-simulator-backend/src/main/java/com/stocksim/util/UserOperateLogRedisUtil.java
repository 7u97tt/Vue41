package com.stocksim.util;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** Redis List：key = {userId}operateLog，最多 50 条 */
@Component
public class UserOperateLogRedisUtil {

    private static final int MAX_LOGS = 50;

    private final JedisUtil jedisUtil;

    public UserOperateLogRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public static String operateLogKey(Long userId) {
        return userId + "operateLog";
    }

    public void pushLogJson(Long userId, String json) {
        String key = operateLogKey(userId);
        jedisUtil.lpush(key, json);
        jedisUtil.ltrim(key, 0, MAX_LOGS - 1);
    }

    public List<String> listRecentLogJson(Long userId) {
        List<String> list = jedisUtil.lrange(operateLogKey(userId), 0, MAX_LOGS - 1);
        return list != null ? list : Collections.emptyList();
    }
}

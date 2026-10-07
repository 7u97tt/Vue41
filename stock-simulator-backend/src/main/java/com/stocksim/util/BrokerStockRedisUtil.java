package com.stocksim.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class BrokerStockRedisUtil {

    public static final String CAN_BORROW_KEY = "broker:stock:canBorrow";

    private final JedisUtil jedisUtil;

    public BrokerStockRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public Integer getCanBorrowCount(String stockCode) {
        String raw = jedisUtil.hget(CAN_BORROW_KEY, stockCode);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }

    public void saveCanBorrowCount(String stockCode, int count) {
        jedisUtil.hset(CAN_BORROW_KEY, stockCode, String.valueOf(count));
    }

    public void syncAll(Map<String, Integer> inventory) {
        Map<String, String> hash = new HashMap<>();
        inventory.forEach((code, cnt) -> hash.put(code, String.valueOf(cnt)));
        if (!hash.isEmpty()) {
            jedisUtil.hmset(CAN_BORROW_KEY, hash);
        }
    }
}

package com.stocksim.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserStockPriceRedisUtil {

    private final JedisUtil jedisUtil;

    public UserStockPriceRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public static String priceKey(Long userId) {
        return userId + "price";
    }

    public Map<String, String> getAllPrices(Long userId) {
        return jedisUtil.hgetAll(priceKey(userId));
    }

    /** Redis hash key={userId}price，field=股票代码 */
    public BigDecimal getPrice(Long userId, String stockCode) {
        String raw = jedisUtil.hget(priceKey(userId), stockCode);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return new BigDecimal(raw.trim()).setScale(2, RoundingMode.HALF_UP);
    }

    public void savePrice(Long userId, String stockCode, BigDecimal price) {
        jedisUtil.hset(priceKey(userId), stockCode, price.toPlainString());
    }

    public void saveAllPrices(Long userId, Map<String, BigDecimal> prices) {
        Map<String, String> hash = new HashMap<>();
        prices.forEach((code, price) -> hash.put(code, price.toPlainString()));
        if (!hash.isEmpty()) {
            jedisUtil.hmset(priceKey(userId), hash);
        }
    }
}

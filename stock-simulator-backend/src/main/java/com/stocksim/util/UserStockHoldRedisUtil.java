package com.stocksim.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 用户持仓 Redis 缓存：{userId}hold（持股数量）、{userId}holdmoney（最近一次买入参考价）。
 */
@Component
public class UserStockHoldRedisUtil {

    private final JedisUtil jedisUtil;

    public UserStockHoldRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public static String holdKey(Long userId) {
        return userId + "hold";
    }

    public static String holdMoneyKey(Long userId) {
        return userId + "holdmoney";
    }

    public void saveHoldCount(Long userId, String stockCode, int holdCount) {
        jedisUtil.hset(holdKey(userId), stockCode, String.valueOf(holdCount));
    }

    /** Redis hash key={userId}hold，field=股票代码 */
    public Integer getHoldCount(Long userId, String stockCode) {
        String raw = jedisUtil.hget(holdKey(userId), stockCode);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }

    /** 保存本次成交价格，供后续盈亏计算参考 */
    public void saveHoldMoneyPrice(Long userId, String stockCode, BigDecimal currentPrice) {
        String value = currentPrice.setScale(2, RoundingMode.HALF_UP).toPlainString();
        jedisUtil.hset(holdMoneyKey(userId), stockCode, value);
    }

    public void deleteHoldCountField(Long userId, String stockCode) {
        jedisUtil.hdel(holdKey(userId), stockCode);
    }

    public void deleteHoldMoneyField(Long userId, String stockCode) {
        jedisUtil.hdel(holdMoneyKey(userId), stockCode);
    }
}

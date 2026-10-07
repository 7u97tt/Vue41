package com.stocksim.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 游戏设置 Redis（Jedis）。
 * <p>Key 示例：{@code "1Difficulty"}（字符串难度）；Hash key 示例：{@code "1info"}（money/time/date）。</p>
 */
@Component
public class GameSettingRedisUtil {

    private final JedisUtil jedisUtil;

    public GameSettingRedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    /** key = {userId}Difficulty，value：0 / 1 / 2 / custom */
    public void saveDifficulty(Long userId, String difficultyCode) {
        jedisUtil.set(userId + "Difficulty", difficultyCode);
    }

    /** hash key = {userId}info，fields: money / time / date */
    public void saveUserInfo(Long userId, BigDecimal money, String gameTime, String gameDate) {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("money", money.toPlainString());
        hash.put("time", gameTime);
        hash.put("date", gameDate);
        jedisUtil.hmset(userId + "info", hash);
    }

    public void updateUserInfoDate(Long userId, String gameDate) {
        jedisUtil.hset(userId + "info", "date", gameDate);
    }

    public void updateUserInfoTime(Long userId, String gameTime) {
        jedisUtil.hset(userId + "info", "time", gameTime);
    }

    public String getInfoField(Long userId, String field) {
        return jedisUtil.hget(userId + "info", field);
    }

    public void updateMoney(Long userId, BigDecimal money) {
        jedisUtil.hset(userId + "info", "money", money.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }
}

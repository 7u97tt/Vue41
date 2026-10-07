package com.stocksim.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksim.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoginTokenRedisUtil {

    private final JedisUtil jedisUtil;
    private final ObjectMapper objectMapper;

    /**
     * 写入 Redis：key=login:token:{token}，value={"userId":1,"username":"test"}
     */
    public void saveLoginToken(String token, Long userId, String username) {
        String key = RedisKeyConstants.loginTokenKey(token);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        try {
            String json = objectMapper.writeValueAsString(payload);
            jedisUtil.set(key, json);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "登录状态写入 Redis 失败");
        }
    }
}

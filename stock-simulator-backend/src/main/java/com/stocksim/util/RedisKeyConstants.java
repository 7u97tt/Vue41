package com.stocksim.util;

public final class RedisKeyConstants {

    /** 登录 token 前缀，完整 key：login:token:{uuid} */
    public static final String LOGIN_TOKEN_PREFIX = "login:token:";

    private RedisKeyConstants() {
    }

    public static String loginTokenKey(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }
}

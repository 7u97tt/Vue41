package com.stocksim.service;

import com.stocksim.vo.GameClockVO;

public interface GameClockService {

    /** Redis {userId}info 优先，否则 MySQL userinfo */
    GameClockVO resolve(Long userId);
}

package com.stocksim.service;

import java.math.BigDecimal;

public interface UserMoneyService {

    BigDecimal getAvailableMoney(Long userId);

    void updateAvailableMoney(Long userId, BigDecimal money);

    /** 允许扣至负数（如融券日手续费） */
    void updateAvailableMoneyAllowNegative(Long userId, BigDecimal money);
}

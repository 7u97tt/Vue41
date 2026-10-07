package com.stocksim.service;

import java.math.BigDecimal;

public interface UserMoneyService {

    BigDecimal getAvailableMoney(Long userId);

    void updateAvailableMoney(Long userId, BigDecimal money);
}

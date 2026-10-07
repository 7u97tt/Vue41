package com.stocksim.service;

import com.stocksim.entity.UserStockPrice;

import java.math.BigDecimal;
import java.util.List;

public interface UserStockPriceService {

    /**
     * 新建用户游戏存档：批量写入预置股票，价格为 [5, 50] 随机值。
     * 若该用户已有股价数据则跳过，保证用户间、存档间隔离。
     */
    void initUserStockArchive(Long userId);

    List<UserStockPrice> getUserAllStock(Long userId);

    void updateStockPrice(Long userId, String stockCode, BigDecimal lastTradePrice);
}

package com.stocksim.service;

import com.stocksim.entity.UserStockHold;

import java.util.List;

public interface UserStockHoldService {

    List<UserStockHold> listByUserId(Long userId);

    UserStockHold getByUserIdAndStockCode(Long userId, String stockCode);

    /**
     * 新增或更新持仓股数；holdCount 为 0 时删除该条记录（若存在）。
     */
    void saveOrUpdateHold(Long userId, String stockCode, int holdCount);

    void deleteHold(Long userId, String stockCode);
}

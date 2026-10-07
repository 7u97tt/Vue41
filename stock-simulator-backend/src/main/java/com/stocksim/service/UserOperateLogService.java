package com.stocksim.service;

import com.stocksim.vo.OperateLogVO;

import java.math.BigDecimal;
import java.util.List;

public interface UserOperateLogService {

    List<OperateLogVO> getOperateLogs(Long userId);

    void recordBuyStock(Long userId,
                        String stockCode,
                        int buyCount,
                        BigDecimal tradeTotal,
                        BigDecimal handFee,
                        BigDecimal actualDebit,
                        BigDecimal afterMoney);

    void recordSellStock(Long userId,
                         String stockCode,
                         int sellCount,
                         BigDecimal tradeAmount,
                         BigDecimal handFee,
                         BigDecimal netReceived,
                         BigDecimal afterMoney);

    void recordShortBorrow(Long userId, String stockCode, int borrowCount, BigDecimal afterMoney);

    void recordShortSell(Long userId,
                         String stockCode,
                         int sellCount,
                         BigDecimal tradeAmount,
                         BigDecimal handFee,
                         BigDecimal netReceived,
                         BigDecimal afterMoney);

    void recordShortReturn(Long userId,
                           String stockCode,
                           int returnCount,
                           BigDecimal totalCost,
                           BigDecimal handFee,
                           BigDecimal actualDebit,
                           BigDecimal afterMoney);

    void recordShortDailyBorrowFee(Long userId,
                                   String stockCode,
                                   BigDecimal borrowBaseTotal,
                                   BigDecimal dailyFee,
                                   BigDecimal afterMoney);
}

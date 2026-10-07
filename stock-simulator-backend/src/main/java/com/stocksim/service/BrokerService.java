package com.stocksim.service;

import com.stocksim.dto.BorrowStockDTO;
import com.stocksim.dto.ReturnStockDTO;
import com.stocksim.dto.ShortSellDTO;
import com.stocksim.vo.UserShortInfoVO;

import java.util.List;

public interface BrokerService {

    void borrowStock(BorrowStockDTO dto);

    void shortSell(ShortSellDTO dto);

    void returnStock(ReturnStockDTO dto);

    List<UserShortInfoVO> getUserShortInfo(Long userId);

    int getCanBorrowCount(String stockCode);

    /** 切换至新交易日前：对未平仓融券按 borrow_base_total 扣当日 5% 手续费 */
    void chargeDailyBorrowFees(Long userId);
}

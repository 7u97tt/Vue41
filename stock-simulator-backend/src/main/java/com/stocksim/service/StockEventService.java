package com.stocksim.service;

import com.stocksim.entity.StockEvent;
import com.stocksim.vo.StockEventVO;

import java.util.List;

public interface StockEventService {

    void saveEvent(StockEvent event);

    List<StockEventVO> getUserRecentEvents(Long userId);
}

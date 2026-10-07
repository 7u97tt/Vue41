package com.stocksim.service;

import com.stocksim.dto.QueryAndUpdateStockPriceDTO;
import com.stocksim.vo.StockBasicVO;
import com.stocksim.vo.StockHistoryVO;
import com.stocksim.vo.StockPriceTickVO;

import java.util.List;

public interface StockService {

    List<String> listIndustries();

    List<StockBasicVO> listByIndustry(String industry);

    StockHistoryVO getHistory(String stockCode);

    StockPriceTickVO queryAndUpdateStockPrice(QueryAndUpdateStockPriceDTO dto);
}

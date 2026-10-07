package com.stocksim.service;

import com.stocksim.dto.SellStockDTO;
import com.stocksim.vo.SellStockVO;

public interface SellStockService {

    SellStockVO sellStock(SellStockDTO dto);
}

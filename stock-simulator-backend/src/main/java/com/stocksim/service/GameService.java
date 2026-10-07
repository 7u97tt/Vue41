package com.stocksim.service;

import com.stocksim.dto.NextTradeDayDTO;
import com.stocksim.vo.NextTradeDayVO;

public interface GameService {

    NextTradeDayVO nextTradeDay(NextTradeDayDTO dto);
}

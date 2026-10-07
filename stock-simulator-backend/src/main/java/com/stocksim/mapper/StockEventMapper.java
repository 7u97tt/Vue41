package com.stocksim.mapper;

import com.stocksim.entity.StockEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockEventMapper {

    int insert(StockEvent event);

    List<StockEvent> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}

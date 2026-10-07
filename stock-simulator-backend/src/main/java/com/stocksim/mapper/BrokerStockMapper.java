package com.stocksim.mapper;

import com.stocksim.entity.BrokerStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BrokerStockMapper {

    BrokerStock selectByStockCode(@Param("stockCode") String stockCode);

    List<BrokerStock> selectAll();

    int updateCanBorrowCount(@Param("stockCode") String stockCode,
                             @Param("canBorrowCount") int canBorrowCount);
}

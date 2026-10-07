package com.stocksim.mapper;

import com.stocksim.entity.StockInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockInfoMapper {

    List<String> selectDistinctIndustries();

    List<StockInfo> selectByIndustry(@Param("industry") String industry);

    StockInfo selectByCode(@Param("stockCode") String stockCode);
}

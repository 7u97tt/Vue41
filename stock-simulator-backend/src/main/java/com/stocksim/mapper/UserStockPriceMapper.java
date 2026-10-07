package com.stocksim.mapper;

import com.stocksim.entity.UserStockPrice;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface UserStockPriceMapper {

    List<UserStockPrice> selectByUserId(@Param("userId") Long userId);

    UserStockPrice selectByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);

    int countByUserId(@Param("userId") Long userId);

    int insert(UserStockPrice record);

    int batchInsert(@Param("list") List<UserStockPrice> list);

    int updateLastTradePrice(@Param("userId") Long userId,
                             @Param("stockCode") String stockCode,
                             @Param("lastTradePrice") BigDecimal lastTradePrice);

    int batchUpdateLastTradePrice(@Param("userId") Long userId,
                                  @Param("list") List<UserStockPrice> list);

    /** 批量更新当日开盘价 open_price（新交易日开盘跳空后落库） */
    int batchUpdateOpenPrice(@Param("userId") Long userId,
                             @Param("list") List<UserStockPrice> list);
}

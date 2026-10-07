package com.stocksim.mapper;

import com.stocksim.entity.UserStockHold;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserStockHoldMapper {

    List<UserStockHold> selectByUserId(@Param("userId") Long userId);

    UserStockHold selectByUserIdAndStockCode(@Param("userId") Long userId,
                                             @Param("stockCode") String stockCode);

    int insert(UserStockHold record);

    int updateHoldCount(UserStockHold record);

    int updateHold(UserStockHold record);

    int deleteByUserIdAndStockCode(@Param("userId") Long userId,
                                   @Param("stockCode") String stockCode);
}

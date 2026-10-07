package com.stocksim.mapper;

import com.stocksim.entity.UserShortPosition;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserShortPositionMapper {

    UserShortPosition selectByUserIdAndStockCode(@Param("userId") Long userId,
                                                  @Param("stockCode") String stockCode);

    List<UserShortPosition> selectByUserId(@Param("userId") Long userId);

    int insert(UserShortPosition row);

    int updatePosition(UserShortPosition row);

    int deleteByUserIdAndStockCode(@Param("userId") Long userId,
                                   @Param("stockCode") String stockCode);
}

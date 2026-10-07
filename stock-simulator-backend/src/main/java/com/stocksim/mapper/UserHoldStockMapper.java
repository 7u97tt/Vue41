package com.stocksim.mapper;

import com.stocksim.entity.UserHoldStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserHoldStockMapper {

    List<UserHoldStock> selectByUserId(@Param("userId") Long userId);

    UserHoldStock selectByUserAndCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);

    int insert(UserHoldStock hold);

    int update(UserHoldStock hold);

    int deleteById(@Param("id") Long id);
}

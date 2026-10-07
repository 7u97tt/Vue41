package com.stocksim.mapper;

import com.stocksim.entity.UserInfo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserInfoMapper {

    UserInfo selectByUserId(@Param("userId") Long userId);

    int insert(UserInfo userInfo);

    int updateGameData(UserInfo userInfo);

    int updateGameDate(@Param("userId") Long userId, @Param("gameDate") String gameDate);

    int updateGameTime(@Param("userId") Long userId, @Param("gameTime") String gameTime);

    int updateMoney(@Param("userId") Long userId, @Param("money") BigDecimal money);
}

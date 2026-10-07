package com.stocksim.mapper;

import com.stocksim.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("userId") Long userId);

    /** 查询 userinfo 表是否存在该 user_id 记录（游戏存档） */
    boolean existsUserInfoByUserId(@Param("userId") Long userId);

    int insert(User user);
}

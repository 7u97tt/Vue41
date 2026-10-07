package com.stocksim.mapper;

import com.stocksim.entity.UserOperateLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserOperateLogMapper {

    int insert(UserOperateLog log);

    List<UserOperateLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}

package com.stocksim.service.impl;

import com.stocksim.entity.UserInfo;
import com.stocksim.mapper.UserInfoMapper;
import com.stocksim.service.GameClockService;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.vo.GameClockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameClockServiceImpl implements GameClockService {

    private final GameSettingRedisUtil gameSettingRedisUtil;
    private final UserInfoMapper userInfoMapper;

    @Override
    public GameClockVO resolve(Long userId) {
        String date = null;
        String time = null;
        try {
            Map<String, String> redisInfo = gameSettingRedisUtil.getUserInfoMap(userId);
            if (redisInfo != null && !redisInfo.isEmpty()) {
                date = redisInfo.get("date");
                time = redisInfo.get("time");
            }
        } catch (Exception ex) {
            log.warn("读取 Redis 游戏时钟失败 userId={}，回退 MySQL：{}", userId, ex.getMessage());
        }

        if (StringUtils.hasText(date) && StringUtils.hasText(time)) {
            return new GameClockVO(date.trim(), time.trim());
        }

        UserInfo info = userInfoMapper.selectByUserId(userId);
        if (info != null && StringUtils.hasText(info.getGameDate()) && StringUtils.hasText(info.getGameTime())) {
            return new GameClockVO(info.getGameDate().trim(), info.getGameTime().trim());
        }
        return new GameClockVO("", "");
    }
}

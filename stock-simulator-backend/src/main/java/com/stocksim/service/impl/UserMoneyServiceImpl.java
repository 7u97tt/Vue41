package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.entity.UserInfo;
import com.stocksim.mapper.UserInfoMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.service.UserMoneyService;
import com.stocksim.util.GameSettingRedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class UserMoneyServiceImpl implements UserMoneyService {

    private final UserInfoMapper userInfoMapper;
    private final UserMapper userMapper;
    private final GameSettingRedisUtil gameSettingRedisUtil;

    @Override
    public BigDecimal getAvailableMoney(Long userId) {
        requireUser(userId);
        UserInfo info = userInfoMapper.selectByUserId(userId);
        if (info == null) {
            throw new BusinessException("用户尚未创建游戏存档");
        }

        String redisMoney = gameSettingRedisUtil.getInfoField(userId, "money");
        if (StringUtils.hasText(redisMoney)) {
            return new BigDecimal(redisMoney.trim()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal dbMoney = info.getMoney().setScale(2, RoundingMode.HALF_UP);
        gameSettingRedisUtil.updateMoney(userId, dbMoney);
        return dbMoney;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvailableMoney(Long userId, BigDecimal money) {
        requireUser(userId);
        if (money == null || money.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("可用金钱不能为负数");
        }
        BigDecimal normalized = money.setScale(2, RoundingMode.HALF_UP);
        int rows = userInfoMapper.updateMoney(userId, normalized);
        if (rows == 0) {
            throw new BusinessException("更新用户金钱失败");
        }
        gameSettingRedisUtil.updateMoney(userId, normalized);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvailableMoneyAllowNegative(Long userId, BigDecimal money) {
        requireUser(userId);
        if (money == null) {
            throw new BusinessException("可用金钱不能为空");
        }
        BigDecimal normalized = money.setScale(2, RoundingMode.HALF_UP);
        int rows = userInfoMapper.updateMoney(userId, normalized);
        if (rows == 0) {
            throw new BusinessException("更新用户金钱失败");
        }
        gameSettingRedisUtil.updateMoney(userId, normalized);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
    }
}

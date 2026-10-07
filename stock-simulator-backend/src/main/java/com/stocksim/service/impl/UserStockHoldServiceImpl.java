package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.entity.User;
import com.stocksim.entity.UserStockHold;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockHoldMapper;
import com.stocksim.service.UserStockHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserStockHoldServiceImpl implements UserStockHoldService {

    private final UserStockHoldMapper userStockHoldMapper;
    private final UserMapper userMapper;

    @Override
    public List<UserStockHold> listByUserId(Long userId) {
        requireUser(userId);
        return userStockHoldMapper.selectByUserId(userId);
    }

    @Override
    public UserStockHold getByUserIdAndStockCode(Long userId, String stockCode) {
        requireUser(userId);
        String code = requireStockCode(stockCode);
        return userStockHoldMapper.selectByUserIdAndStockCode(userId, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateHold(Long userId, String stockCode, int holdCount) {
        requireUser(userId);
        String code = requireStockCode(stockCode);
        if (holdCount < 0) {
            throw new BusinessException("持股股数不能为负数");
        }

        if (holdCount == 0) {
            userStockHoldMapper.deleteByUserIdAndStockCode(userId, code);
            return;
        }

        UserStockHold existing = userStockHoldMapper.selectByUserIdAndStockCode(userId, code);
        if (existing == null) {
            UserStockHold row = new UserStockHold();
            row.setUserId(userId);
            row.setStockCode(code);
            row.setHoldCount(holdCount);
            row.setBuyTotalCost(BigDecimal.ZERO);
            userStockHoldMapper.insert(row);
            return;
        }

        existing.setHoldCount(holdCount);
        int rows = userStockHoldMapper.updateHoldCount(existing);
        if (rows == 0) {
            throw new BusinessException("更新持仓失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHold(Long userId, String stockCode) {
        requireUser(userId);
        String code = requireStockCode(stockCode);
        userStockHoldMapper.deleteByUserIdAndStockCode(userId, code);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
    }

    private String requireStockCode(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            throw new BusinessException("stockCode 不能为空");
        }
        return stockCode.trim();
    }
}

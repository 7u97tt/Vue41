package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.dto.BuyStockDTO;
import com.stocksim.entity.UserStockHold;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockHoldMapper;
import com.stocksim.service.BuyStockService;
import com.stocksim.service.UserMoneyService;
import com.stocksim.service.UserOperateLogService;
import com.stocksim.util.UserStockHoldRedisUtil;
import com.stocksim.util.UserStockPriceRedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BuyStockServiceImpl implements BuyStockService {

    private static final BigDecimal DEFAULT_HAND_FEE_RATE = new BigDecimal("0.003");

    private final UserMapper userMapper;
    private final UserStockHoldMapper userStockHoldMapper;
    private final UserMoneyService userMoneyService;
    private final UserStockPriceRedisUtil userStockPriceRedisUtil;
    private final UserStockHoldRedisUtil userStockHoldRedisUtil;
    private final UserOperateLogService userOperateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void buyStock(BuyStockDTO dto) {
        // 1. 参数校验：buyCount 必须为大于 0 的正整数
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (dto.getUserId() == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (!StringUtils.hasText(dto.getStockCode())) {
            throw new BusinessException("stockCode 不能为空");
        }
        if (dto.getBuyCount() == null || dto.getBuyCount() <= 0) {
            throw new BusinessException("buyCount 必须为大于 0 的正整数");
        }
        Long userId = dto.getUserId();
        String stockCode = dto.getStockCode().trim();
        int buyCount = dto.getBuyCount();

        BigDecimal handFeeRate = dto.getHandFeeRate() != null
                ? dto.getHandFeeRate()
                : DEFAULT_HAND_FEE_RATE;
        if (handFeeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("handFeeRate 不能为负数");
        }

        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. Redis：{userId}price 取实时股价；{userId}info 取可用金钱（经 UserMoneyService 读 Redis/MySQL）
        BigDecimal currentPrice = userStockPriceRedisUtil.getPrice(userId, stockCode);
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("无股价数据，请稍后再试");
        }

        BigDecimal availableMoney = userMoneyService.getAvailableMoney(userId);

        // 3. 金额计算（手续费不计入 buy_total_cost）
        BigDecimal tradeTotal = currentPrice
                .multiply(BigDecimal.valueOf(buyCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal handFee = tradeTotal
                .multiply(handFeeRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualDebit = tradeTotal.add(handFee).setScale(2, RoundingMode.HALF_UP);

        // 4. 可用金钱必须 ≥ 实际扣款总金额（成交总额 + 手续费）
        if (availableMoney.compareTo(actualDebit) < 0) {
            throw new BusinessException("可用资金不足，无法完成买入");
        }

        // 5. 扣减用户金钱，同步 MySQL userinfo.money 与 Redis {userId}info field=money
        BigDecimal newMoney = availableMoney.subtract(actualDebit).setScale(2, RoundingMode.HALF_UP);
        userMoneyService.updateAvailableMoney(userId, newMoney);

        // 6. 更新 MySQL user_stock_hold（buy_total_cost 仅累加成交总额 tradeTotal）
        UserStockHold existing = userStockHoldMapper.selectByUserIdAndStockCode(userId, stockCode);
        int totalHoldCount;
        if (existing == null) {
            UserStockHold row = new UserStockHold();
            row.setUserId(userId);
            row.setStockCode(stockCode);
            row.setHoldCount(buyCount);
            row.setBuyTotalCost(tradeTotal);
            userStockHoldMapper.insert(row);
            totalHoldCount = buyCount;
        } else {
            int newCount = existing.getHoldCount() + buyCount;
            BigDecimal oldCost = existing.getBuyTotalCost() == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : existing.getBuyTotalCost();
            existing.setHoldCount(newCount);
            existing.setBuyTotalCost(oldCost.add(tradeTotal).setScale(2, RoundingMode.HALF_UP));
            userStockHoldMapper.updateHold(existing);
            totalHoldCount = newCount;
        }

        // 7. 同步 Redis 持仓：{userId}hold 总股数；{userId}holdmoney 本次成交价
        userStockHoldRedisUtil.saveHoldCount(userId, stockCode, totalHoldCount);
        userStockHoldRedisUtil.saveHoldMoneyPrice(userId, stockCode, currentPrice);

        userOperateLogService.recordBuyStock(
                userId, stockCode, buyCount, tradeTotal, handFee, actualDebit, newMoney);
    }
}

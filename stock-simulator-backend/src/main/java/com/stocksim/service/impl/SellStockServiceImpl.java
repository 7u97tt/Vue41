package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.dto.SellStockDTO;
import com.stocksim.entity.UserStockHold;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockHoldMapper;
import com.stocksim.service.SellStockService;
import com.stocksim.service.UserMoneyService;
import com.stocksim.service.UserOperateLogService;
import com.stocksim.util.UserStockHoldRedisUtil;
import com.stocksim.util.UserStockPriceRedisUtil;
import com.stocksim.vo.SellStockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class SellStockServiceImpl implements SellStockService {

    private static final BigDecimal HAND_FEE_RATE = new BigDecimal("0.003");

    private final UserMapper userMapper;
    private final UserStockHoldMapper userStockHoldMapper;
    private final UserMoneyService userMoneyService;
    private final UserStockPriceRedisUtil userStockPriceRedisUtil;
    private final UserStockHoldRedisUtil userStockHoldRedisUtil;
    private final UserOperateLogService userOperateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SellStockVO sellStock(SellStockDTO dto) {
        // 1. 参数校验
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (dto.getUserId() == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (!StringUtils.hasText(dto.getStockCode())) {
            throw new BusinessException("stockCode 不能为空");
        }
        if (dto.getSellCount() == null || dto.getSellCount() <= 0) {
            throw new BusinessException("sellCount 必须为大于 0 的正整数");
        }

        Long userId = dto.getUserId();
        String stockCode = dto.getStockCode().trim();
        int sellCount = dto.getSellCount();

        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. Redis：可用金钱、实时股价、持仓股数
        BigDecimal availableMoney = userMoneyService.getAvailableMoney(userId);

        BigDecimal currentPrice = userStockPriceRedisUtil.getPrice(userId, stockCode);
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("无股价数据，请稍后再试");
        }

        Integer holdNumFromRedis = userStockHoldRedisUtil.getHoldCount(userId, stockCode);
        UserStockHold existing = userStockHoldMapper.selectByUserIdAndStockCode(userId, stockCode);
        int holdNum = holdNumFromRedis != null
                ? holdNumFromRedis
                : (existing != null ? existing.getHoldCount() : 0);

        // 3. 卖出股数不能超过持有数量
        if (sellCount > holdNum) {
            throw new BusinessException("持股数量不足，无法卖出");
        }
        if (existing == null || existing.getHoldCount() <= 0) {
            throw new BusinessException("持股数量不足，无法卖出");
        }

        int originalHoldCount = existing.getHoldCount();
        if (sellCount > originalHoldCount) {
            throw new BusinessException("持股数量不足，无法卖出");
        }

        // 4. 金额计算
        BigDecimal tradeAmount = currentPrice
                .multiply(BigDecimal.valueOf(sellCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal handFee = tradeAmount
                .multiply(HAND_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netReceived = tradeAmount.subtract(handFee).setScale(2, RoundingMode.HALF_UP);

        // 5. 增加用户可用金钱，MySQL userinfo.money + Redis {userId}info.money
        BigDecimal newMoney = availableMoney.add(netReceived).setScale(2, RoundingMode.HALF_UP);
        userMoneyService.updateAvailableMoney(userId, newMoney);

        // 6. 更新 MySQL user_stock_hold：按比例扣减 buy_total_cost
        BigDecimal oldCost = existing.getBuyTotalCost() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : existing.getBuyTotalCost();
        BigDecimal deductCost = oldCost
                .multiply(BigDecimal.valueOf(sellCount))
                .divide(BigDecimal.valueOf(originalHoldCount), 2, RoundingMode.HALF_UP);
        BigDecimal newBuyTotalCost = oldCost.subtract(deductCost).setScale(2, RoundingMode.HALF_UP);
        int newHoldCount = originalHoldCount - sellCount;

        if (newHoldCount > 0) {
            existing.setHoldCount(newHoldCount);
            existing.setBuyTotalCost(newBuyTotalCost);
            userStockHoldMapper.updateHold(existing);
        } else {
            userStockHoldMapper.deleteByUserIdAndStockCode(userId, stockCode);
        }

        // 7. Redis {userId}hold / {userId}holdmoney
        if (newHoldCount <= 0) {
            userStockHoldRedisUtil.deleteHoldCountField(userId, stockCode);
            userStockHoldRedisUtil.deleteHoldMoneyField(userId, stockCode);
        } else {
            userStockHoldRedisUtil.saveHoldCount(userId, stockCode, newHoldCount);
        }

        userOperateLogService.recordSellStock(
                userId, stockCode, sellCount, tradeAmount, handFee, netReceived, newMoney);

        return new SellStockVO(sellCount, tradeAmount, handFee, netReceived);
    }
}

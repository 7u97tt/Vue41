package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.constant.GameStockCatalog;
import com.stocksim.entity.User;
import com.stocksim.entity.UserStockPrice;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockPriceMapper;
import com.stocksim.service.UserStockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserStockPriceServiceImpl implements UserStockPriceService {

    private static final BigDecimal MIN_PRICE = new BigDecimal("5.00");
    private static final BigDecimal MAX_PRICE = new BigDecimal("50.00");

    private final UserStockPriceMapper userStockPriceMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserStockArchive(Long userId) {
        requireUser(userId);
        if (userStockPriceMapper.countByUserId(userId) > 0) {
            return;
        }

        List<UserStockPrice> batch = new ArrayList<>();
        for (GameStockCatalog.StockItem item : GameStockCatalog.all()) {
            UserStockPrice row = new UserStockPrice();
            row.setUserId(userId);
            row.setStockName(item.getStockName());
            row.setStockCode(item.getStockCode());
            BigDecimal initial = randomInitialPrice();
            row.setLastTradePrice(initial);
            row.setOpenPrice(initial);
            batch.add(row);
        }
        userStockPriceMapper.batchInsert(batch);
    }

    @Override
    public List<UserStockPrice> getUserAllStock(Long userId) {
        requireUser(userId);
        return userStockPriceMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockPrice(Long userId, String stockCode, BigDecimal lastTradePrice) {
        requireUser(userId);
        if (!StringUtils.hasText(stockCode)) {
            throw new BusinessException("stockCode 不能为空");
        }
        if (lastTradePrice == null || lastTradePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("lastTradePrice 必须大于 0");
        }

        String code = stockCode.trim();
        UserStockPrice existing = userStockPriceMapper.selectByUserIdAndStockCode(userId, code);
        if (existing == null) {
            throw new BusinessException("该用户不存在此股票: " + code);
        }

        BigDecimal normalized = lastTradePrice.setScale(2, RoundingMode.HALF_UP);
        int rows = userStockPriceMapper.updateLastTradePrice(userId, code, normalized);
        if (rows == 0) {
            throw new BusinessException("更新失败");
        }
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

    private BigDecimal randomInitialPrice() {
        double raw = ThreadLocalRandom.current().nextDouble(
                MIN_PRICE.doubleValue(),
                MAX_PRICE.doubleValue() + 0.001
        );
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }
}

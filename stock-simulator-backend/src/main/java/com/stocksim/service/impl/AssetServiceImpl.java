package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.entity.UserStockHold;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockHoldMapper;
import com.stocksim.mapper.UserStockPriceMapper;
import com.stocksim.entity.UserStockPrice;
import com.stocksim.service.AssetService;
import com.stocksim.service.UserMoneyService;
import com.stocksim.util.UserStockPriceRedisUtil;
import com.stocksim.vo.HoldAssetItemVO;
import com.stocksim.vo.MyAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final UserMoneyService userMoneyService;
    private final UserStockHoldMapper userStockHoldMapper;
    private final UserStockPriceRedisUtil userStockPriceRedisUtil;
    private final UserStockPriceMapper userStockPriceMapper;
    private final UserMapper userMapper;

    @Override
    public MyAssetVO getMyAsset(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        BigDecimal availableMoney = userMoneyService.getAvailableMoney(userId);
        List<UserStockHold> holds = userStockHoldMapper.selectByUserId(userId);
        Map<String, String> redisPrices = userStockPriceRedisUtil.getAllPrices(userId);
        Map<String, BigDecimal> dbPriceFallback = loadDbPriceFallback(userId);

        BigDecimal holdMarketValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBuyCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProfit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Map<String, String> stockNames = loadStockNames(userId);
        List<HoldAssetItemVO> holdItems = new ArrayList<>();

        for (UserStockHold hold : holds) {
            int qty = hold.getHoldCount() == null ? 0 : hold.getHoldCount();
            if (qty <= 0) {
                continue;
            }
            BigDecimal buyCost = hold.getBuyTotalCost() == null
                    ? BigDecimal.ZERO
                    : hold.getBuyTotalCost().setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = resolvePrice(hold.getStockCode(), redisPrices, dbPriceFallback);

            BigDecimal lineMarket = price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            holdMarketValue = holdMarketValue.add(lineMarket);

            totalBuyCost = totalBuyCost.add(buyCost);

            BigDecimal avgCost = buyCost.divide(BigDecimal.valueOf(qty), 4, RoundingMode.HALF_UP);
            BigDecimal lineProfit = price.subtract(avgCost)
                    .multiply(BigDecimal.valueOf(qty))
                    .setScale(2, RoundingMode.HALF_UP);
            totalProfit = totalProfit.add(lineProfit);

            HoldAssetItemVO item = new HoldAssetItemVO();
            item.setStockCode(hold.getStockCode());
            item.setStockName(stockNames.getOrDefault(hold.getStockCode(), hold.getStockCode()));
            item.setHoldCount(qty);
            item.setBuyCostPrice(avgCost.setScale(2, RoundingMode.HALF_UP));
            item.setCurrentPrice(price);
            item.setMarketValue(lineMarket);
            item.setProfitAmount(lineProfit);
            if (avgCost.compareTo(BigDecimal.ZERO) > 0) {
                item.setProfitRate(price.subtract(avgCost)
                        .divide(avgCost, 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP));
            } else {
                item.setProfitRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            holdItems.add(item);
        }

        holdMarketValue = holdMarketValue.setScale(2, RoundingMode.HALF_UP);
        totalBuyCost = totalBuyCost.setScale(2, RoundingMode.HALF_UP);
        totalProfit = totalProfit.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAsset = availableMoney.add(holdMarketValue).setScale(2, RoundingMode.HALF_UP);

        BigDecimal profitRate = null;
        if (totalBuyCost.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = holdMarketValue.subtract(totalBuyCost)
                    .divide(totalBuyCost, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        MyAssetVO vo = new MyAssetVO();
        vo.setAvailableMoney(availableMoney);
        vo.setHoldMarketValue(holdMarketValue);
        vo.setTotalAsset(totalAsset);
        vo.setTotalBuyCost(totalBuyCost);
        vo.setTotalProfit(totalProfit);
        vo.setProfitRate(profitRate);
        vo.setHoldList(holdItems);
        return vo;
    }

    private Map<String, String> loadStockNames(Long userId) {
        Map<String, String> map = new HashMap<>();
        for (UserStockPrice row : userStockPriceMapper.selectByUserId(userId)) {
            map.put(row.getStockCode(), row.getStockName());
        }
        return map;
    }

    private Map<String, BigDecimal> loadDbPriceFallback(Long userId) {
        Map<String, BigDecimal> map = new HashMap<>();
        List<UserStockPrice> list = userStockPriceMapper.selectByUserId(userId);
        for (UserStockPrice row : list) {
            map.put(row.getStockCode(), row.getLastTradePrice());
        }
        return map;
    }

    private BigDecimal resolvePrice(String stockCode,
                                    Map<String, String> redisPrices,
                                    Map<String, BigDecimal> dbFallback) {
        String raw = redisPrices.get(stockCode);
        if (StringUtils.hasText(raw)) {
            return new BigDecimal(raw.trim()).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fallback = dbFallback.get(stockCode);
        if (fallback != null) {
            return fallback.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}

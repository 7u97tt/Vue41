package com.stocksim.service;

import com.stocksim.common.BusinessException;
import com.stocksim.context.UserContext;
import com.stocksim.dto.TradeRequest;
import com.stocksim.entity.UserStockHold;
import com.stocksim.entity.UserStockPrice;
import com.stocksim.mapper.UserStockHoldMapper;
import com.stocksim.mapper.UserStockPriceMapper;
import com.stocksim.vo.HoldStockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HoldStockService {

    private final UserStockHoldMapper userStockHoldMapper;
    private final UserStockPriceMapper userStockPriceMapper;
    private final UserMoneyService userMoneyService;

    public List<HoldStockVO> listMyHolds(Long userId) {
        Long uid = resolveUserId(userId);
        List<UserStockHold> list = userStockHoldMapper.selectByUserId(uid);
        Map<String, String> nameMap = loadStockNames(uid);

        List<HoldStockVO> result = new ArrayList<>();
        for (UserStockHold h : list) {
            HoldStockVO vo = new HoldStockVO();
            vo.setStockCode(h.getStockCode());
            vo.setStockName(nameMap.getOrDefault(h.getStockCode(), h.getStockCode()));
            vo.setQuantity(h.getHoldCount());
            BigDecimal buyCost = h.getBuyTotalCost() == null ? BigDecimal.ZERO : h.getBuyTotalCost();
            BigDecimal avg = h.getHoldCount() == null || h.getHoldCount() == 0
                    ? BigDecimal.ZERO
                    : buyCost.divide(BigDecimal.valueOf(h.getHoldCount()), 4, RoundingMode.HALF_UP);
            vo.setAvgPrice(avg.setScale(2, RoundingMode.HALF_UP));
            vo.setMarketValue(buyCost.setScale(2, RoundingMode.HALF_UP));
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void buy(TradeRequest req) {
        Long userId = resolveUserId(req.getUserId());
        String code = requireCode(req.getStockCode());
        int qty = req.getQuantity();
        BigDecimal price = req.getPrice().setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("成交价格必须大于 0");
        }

        BigDecimal amount = price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal money = userMoneyService.getAvailableMoney(userId);
        if (money.compareTo(amount) < 0) {
            throw new BusinessException("可用资金不足，无法完成买入");
        }

        UserStockHold existing = userStockHoldMapper.selectByUserIdAndStockCode(userId, code);
        if (existing == null) {
            UserStockHold row = new UserStockHold();
            row.setUserId(userId);
            row.setStockCode(code);
            row.setHoldCount(qty);
            row.setBuyTotalCost(amount);
            userStockHoldMapper.insert(row);
        } else {
            int newCount = existing.getHoldCount() + qty;
            BigDecimal oldCost = existing.getBuyTotalCost() == null ? BigDecimal.ZERO : existing.getBuyTotalCost();
            existing.setHoldCount(newCount);
            existing.setBuyTotalCost(oldCost.add(amount).setScale(2, RoundingMode.HALF_UP));
            userStockHoldMapper.updateHold(existing);
        }

        userMoneyService.updateAvailableMoney(userId, money.subtract(amount));
    }

    @Transactional(rollbackFor = Exception.class)
    public void sell(TradeRequest req) {
        Long userId = resolveUserId(req.getUserId());
        String code = requireCode(req.getStockCode());
        int qty = req.getQuantity();
        BigDecimal price = req.getPrice().setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("成交价格必须大于 0");
        }

        UserStockHold existing = userStockHoldMapper.selectByUserIdAndStockCode(userId, code);
        if (existing == null || existing.getHoldCount() == null || existing.getHoldCount() < qty) {
            throw new BusinessException("卖出数量不能超过当前持有数量");
        }

        BigDecimal amount = price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        int remain = existing.getHoldCount() - qty;
        BigDecimal buyCost = existing.getBuyTotalCost() == null ? BigDecimal.ZERO : existing.getBuyTotalCost();
        BigDecimal avgCost = buyCost.divide(BigDecimal.valueOf(existing.getHoldCount()), 8, RoundingMode.HALF_UP);
        BigDecimal soldCost = avgCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

        if (remain <= 0) {
            userStockHoldMapper.deleteByUserIdAndStockCode(userId, code);
        } else {
            existing.setHoldCount(remain);
            existing.setBuyTotalCost(buyCost.subtract(soldCost).setScale(2, RoundingMode.HALF_UP));
            userStockHoldMapper.updateHold(existing);
        }

        BigDecimal money = userMoneyService.getAvailableMoney(userId);
        userMoneyService.updateAvailableMoney(userId, money.add(amount));
    }

    private Long resolveUserId(Long userId) {
        if (userId != null) {
            return userId;
        }
        Long ctx = UserContext.getUserId();
        if (ctx == null) {
            throw new BusinessException("userId 不能为空");
        }
        return ctx;
    }

    private String requireCode(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            throw new BusinessException("股票代码不能为空");
        }
        return stockCode.trim();
    }

    private Map<String, String> loadStockNames(Long userId) {
        Map<String, String> map = new HashMap<>();
        List<UserStockPrice> prices = userStockPriceMapper.selectByUserId(userId);
        for (UserStockPrice p : prices) {
            map.put(p.getStockCode(), p.getStockName());
        }
        return map;
    }
}

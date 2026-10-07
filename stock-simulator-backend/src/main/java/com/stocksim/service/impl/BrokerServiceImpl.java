package com.stocksim.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksim.common.BusinessException;
import com.stocksim.dto.BorrowStockDTO;
import com.stocksim.dto.ReturnStockDTO;
import com.stocksim.dto.ShortSellDTO;
import com.stocksim.entity.BrokerStock;
import com.stocksim.entity.UserShortPosition;
import com.stocksim.mapper.BrokerStockMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserShortPositionMapper;
import com.stocksim.service.BrokerService;
import com.stocksim.service.UserMoneyService;
import com.stocksim.service.UserOperateLogService;
import com.stocksim.util.BrokerStockRedisUtil;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.util.UserShortPosRedisUtil;
import com.stocksim.util.UserStockPriceRedisUtil;
import com.stocksim.vo.UserShortInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BrokerServiceImpl implements BrokerService {

    private static final BigDecimal HAND_FEE_RATE = new BigDecimal("0.003");
    private static final BigDecimal DAILY_BORROW_FEE_RATE = new BigDecimal("0.05");

    private final UserMapper userMapper;
    private final BrokerStockMapper brokerStockMapper;
    private final UserShortPositionMapper userShortPositionMapper;
    private final UserMoneyService userMoneyService;
    private final UserStockPriceRedisUtil userStockPriceRedisUtil;
    private final BrokerStockRedisUtil brokerStockRedisUtil;
    private final UserShortPosRedisUtil userShortPosRedisUtil;
    private final UserOperateLogService userOperateLogService;
    private final GameSettingRedisUtil gameSettingRedisUtil;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void borrowStock(BorrowStockDTO dto) {
        validateUser(dto.getUserId());
        if (!StringUtils.hasText(dto.getStockCode())) {
            throw new BusinessException("stockCode 不能为空");
        }
        if (dto.getBorrowCount() == null || dto.getBorrowCount() <= 0) {
            throw new BusinessException("borrowCount 必须为大于 0 的正整数");
        }
        Long userId = dto.getUserId();
        String stockCode = dto.getStockCode().trim();
        int borrowCount = dto.getBorrowCount();

        int canBorrow = getCanBorrowCount(stockCode);
        if (borrowCount > canBorrow) {
            throw new BusinessException("券商可借库存不足，无法借券");
        }

        BigDecimal currentPrice = requirePrice(userId, stockCode);
        GameClock clock = resolveGameClock(userId);

        int newCanBorrow = canBorrow - borrowCount;
        brokerStockMapper.updateCanBorrowCount(stockCode, newCanBorrow);
        brokerStockRedisUtil.saveCanBorrowCount(stockCode, newCanBorrow);

        BigDecimal borrowSliceBase = currentPrice.multiply(BigDecimal.valueOf(borrowCount))
                .setScale(2, RoundingMode.HALF_UP);

        UserShortPosition existing = userShortPositionMapper.selectByUserIdAndStockCode(userId, stockCode);
        if (existing == null) {
            UserShortPosition row = new UserShortPosition();
            row.setUserId(userId);
            row.setStockCode(stockCode);
            row.setShortCount(borrowCount);
            row.setShortSoldCount(0);
            row.setNotSoldCount(borrowCount);
            row.setBorrowPrice(currentPrice);
            row.setBorrowBaseTotal(borrowSliceBase);
            row.setCreateGameDate(clock.gameDate());
            row.setCreateGameTime(clock.gameTime());
            userShortPositionMapper.insert(row);
            syncShortPosRedis(userId, row);
        } else {
            int newShortCount = existing.getShortCount() + borrowCount;
            int newNotSold = existing.getNotSoldCount() + borrowCount;
            BigDecimal oldPrice = existing.getBorrowPrice();
            BigDecimal weighted = oldPrice
                    .multiply(BigDecimal.valueOf(existing.getShortCount()))
                    .add(currentPrice.multiply(BigDecimal.valueOf(borrowCount)))
                    .divide(BigDecimal.valueOf(newShortCount), 2, RoundingMode.HALF_UP);
            BigDecimal oldBase = existing.getBorrowBaseTotal() == null
                    ? BigDecimal.ZERO
                    : existing.getBorrowBaseTotal();
            existing.setShortCount(newShortCount);
            existing.setNotSoldCount(newNotSold);
            existing.setBorrowPrice(weighted);
            existing.setBorrowBaseTotal(oldBase.add(borrowSliceBase).setScale(2, RoundingMode.HALF_UP));
            userShortPositionMapper.updatePosition(existing);
            syncShortPosRedis(userId, existing);
        }

        BigDecimal money = userMoneyService.getAvailableMoney(userId);
        userOperateLogService.recordShortBorrow(userId, stockCode, borrowCount, money);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shortSell(ShortSellDTO dto) {
        validateUser(dto.getUserId());
        if (!StringUtils.hasText(dto.getStockCode())) {
            throw new BusinessException("stockCode 不能为空");
        }
        if (dto.getSellCount() == null || dto.getSellCount() <= 0) {
            throw new BusinessException("sellCount 必须为大于 0 的正整数");
        }
        Long userId = dto.getUserId();
        String stockCode = dto.getStockCode().trim();
        int sellCount = dto.getSellCount();

        UserShortPosition pos = requireShortPosition(userId, stockCode);
        if (pos.getNotSoldCount() < sellCount) {
            throw new BusinessException("未卖出的融券股数不足，无法融券卖出");
        }

        BigDecimal currentPrice = requirePrice(userId, stockCode);
        BigDecimal tradeAmount = currentPrice.multiply(BigDecimal.valueOf(sellCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal handFee = tradeAmount.multiply(HAND_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netReceived = tradeAmount.subtract(handFee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal available = userMoneyService.getAvailableMoney(userId);
        BigDecimal newMoney = available.add(netReceived).setScale(2, RoundingMode.HALF_UP);
        userMoneyService.updateAvailableMoney(userId, newMoney);

        pos.setNotSoldCount(pos.getNotSoldCount() - sellCount);
        pos.setShortSoldCount(pos.getShortSoldCount() + sellCount);
        userShortPositionMapper.updatePosition(pos);
        syncShortPosRedis(userId, pos);

        userOperateLogService.recordShortSell(userId, stockCode, sellCount, tradeAmount, handFee, netReceived, newMoney);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnStock(ReturnStockDTO dto) {
        validateUser(dto.getUserId());
        if (!StringUtils.hasText(dto.getStockCode())) {
            throw new BusinessException("stockCode 不能为空");
        }
        if (dto.getReturnCount() == null || dto.getReturnCount() <= 0) {
            throw new BusinessException("returnCount 必须为大于 0 的正整数");
        }
        Long userId = dto.getUserId();
        String stockCode = dto.getStockCode().trim();
        int returnCount = dto.getReturnCount();

        UserShortPosition pos = requireShortPosition(userId, stockCode);
        if (pos.getShortSoldCount() < returnCount) {
            throw new BusinessException("已卖出的融券数量不足，无法归还平仓");
        }

        BigDecimal currentPrice = requirePrice(userId, stockCode);
        BigDecimal totalCost = currentPrice.multiply(BigDecimal.valueOf(returnCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal handFee = totalCost.multiply(HAND_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualDebit = totalCost.add(handFee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal available = userMoneyService.getAvailableMoney(userId);
        if (available.compareTo(actualDebit) < 0) {
            throw new BusinessException("可用资金不足，无法归还平仓");
        }
        BigDecimal newMoney = available.subtract(actualDebit).setScale(2, RoundingMode.HALF_UP);
        userMoneyService.updateAvailableMoney(userId, newMoney);

        int oldShortCount = pos.getShortCount();
        pos.setShortSoldCount(pos.getShortSoldCount() - returnCount);
        pos.setShortCount(oldShortCount - returnCount);
        if (pos.getShortCount() > 0 && pos.getBorrowBaseTotal() != null && oldShortCount > 0) {
            BigDecimal ratio = BigDecimal.valueOf(pos.getShortCount())
                    .divide(BigDecimal.valueOf(oldShortCount), 8, RoundingMode.HALF_UP);
            pos.setBorrowBaseTotal(
                    pos.getBorrowBaseTotal().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        }
        if (pos.getShortCount() <= 0) {
            userShortPositionMapper.deleteByUserIdAndStockCode(userId, stockCode);
            userShortPosRedisUtil.deleteField(userId, stockCode);
        } else {
            userShortPositionMapper.updatePosition(pos);
            syncShortPosRedis(userId, pos);
        }

        int canBorrow = getCanBorrowCount(stockCode);
        int restored = canBorrow + returnCount;
        brokerStockMapper.updateCanBorrowCount(stockCode, restored);
        brokerStockRedisUtil.saveCanBorrowCount(stockCode, restored);

        userOperateLogService.recordShortReturn(
                userId, stockCode, returnCount, totalCost, handFee, actualDebit, newMoney);
    }

    @Override
    public List<UserShortInfoVO> getUserShortInfo(Long userId) {
        validateUser(userId);
        Map<String, String> redisMap = userShortPosRedisUtil.getAll(userId);
        if (redisMap != null && !redisMap.isEmpty()) {
            List<UserShortInfoVO> list = new ArrayList<>();
            for (String json : redisMap.values()) {
                if (!StringUtils.hasText(json)) {
                    continue;
                }
                try {
                    list.add(objectMapper.readValue(json, UserShortInfoVO.class));
                } catch (Exception ignored) {
                    /* skip */
                }
            }
            if (!list.isEmpty()) {
                list.sort((a, b) -> a.getStockCode().compareTo(b.getStockCode()));
                return list;
            }
        }
        return userShortPositionMapper.selectByUserId(userId).stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void chargeDailyBorrowFees(Long userId) {
        validateUser(userId);
        List<UserShortPosition> positions = userShortPositionMapper.selectByUserId(userId);
        if (positions.isEmpty()) {
            return;
        }
        BigDecimal money = userMoneyService.getAvailableMoney(userId);
        for (UserShortPosition pos : positions) {
            if (pos.getShortCount() == null || pos.getShortCount() <= 0) {
                continue;
            }
            BigDecimal base = resolveBorrowBaseTotal(pos);
            if (base.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal dailyFee = base.multiply(DAILY_BORROW_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            if (dailyFee.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            money = money.subtract(dailyFee).setScale(2, RoundingMode.HALF_UP);
            userMoneyService.updateAvailableMoneyAllowNegative(userId, money);
            userOperateLogService.recordShortDailyBorrowFee(
                    userId, pos.getStockCode(), base, dailyFee, money);
        }
    }

    private static BigDecimal resolveBorrowBaseTotal(UserShortPosition pos) {
        if (pos.getBorrowBaseTotal() != null && pos.getBorrowBaseTotal().compareTo(BigDecimal.ZERO) > 0) {
            return pos.getBorrowBaseTotal().setScale(2, RoundingMode.HALF_UP);
        }
        if (pos.getBorrowPrice() != null && pos.getShortCount() != null && pos.getShortCount() > 0) {
            return pos.getBorrowPrice()
                    .multiply(BigDecimal.valueOf(pos.getShortCount()))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int getCanBorrowCount(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            throw new BusinessException("stockCode 不能为空");
        }
        String code = stockCode.trim();
        Integer cached = brokerStockRedisUtil.getCanBorrowCount(code);
        if (cached != null) {
            return cached;
        }
        BrokerStock row = brokerStockMapper.selectByStockCode(code);
        if (row == null) {
            throw new BusinessException("券商未配置该股票库存");
        }
        brokerStockRedisUtil.saveCanBorrowCount(code, row.getCanBorrowCount());
        return row.getCanBorrowCount();
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
    }

    private BigDecimal requirePrice(Long userId, String stockCode) {
        BigDecimal price = userStockPriceRedisUtil.getPrice(userId, stockCode);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("无股价数据，请稍后再试");
        }
        return price;
    }

    private UserShortPosition requireShortPosition(Long userId, String stockCode) {
        UserShortPosition pos = userShortPositionMapper.selectByUserIdAndStockCode(userId, stockCode);
        if (pos == null) {
            throw new BusinessException("暂无该股票融券负债");
        }
        return pos;
    }

    private void syncShortPosRedis(Long userId, UserShortPosition pos) {
        try {
            UserShortInfoVO vo = toVo(pos);
            String json = objectMapper.writeValueAsString(vo);
            userShortPosRedisUtil.saveFieldJson(userId, pos.getStockCode(), json);
        } catch (Exception e) {
            throw new BusinessException("同步融券缓存失败");
        }
    }

    private UserShortInfoVO toVo(UserShortPosition pos) {
        UserShortInfoVO vo = new UserShortInfoVO();
        vo.setStockCode(pos.getStockCode());
        vo.setShortCount(pos.getShortCount());
        vo.setShortSoldCount(pos.getShortSoldCount());
        vo.setNotSoldCount(pos.getNotSoldCount());
        vo.setBorrowPrice(pos.getBorrowPrice());
        vo.setBorrowBaseTotal(resolveBorrowBaseTotal(pos));
        return vo;
    }

    private GameClock resolveGameClock(Long userId) {
        String date = gameSettingRedisUtil.getInfoField(userId, "date");
        String time = gameSettingRedisUtil.getInfoField(userId, "time");
        if (StringUtils.hasText(date) && StringUtils.hasText(time)) {
            return new GameClock(date.trim(), time.trim());
        }
        return new GameClock("", "");
    }

    private record GameClock(String gameDate, String gameTime) {
    }
}

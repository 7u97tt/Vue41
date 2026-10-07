package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.dto.NextTradeDayDTO;
import com.stocksim.entity.User;
import com.stocksim.entity.UserInfo;
import com.stocksim.entity.UserStockPrice;
import com.stocksim.mapper.UserInfoMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockPriceMapper;
import com.stocksim.service.GameService;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.util.StockPriceCalcUtil;
import com.stocksim.util.UserStockPriceRedisUtil;
import com.stocksim.vo.NextTradeDayVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String OPEN_TIME = "09:30";

    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserStockPriceMapper userStockPriceMapper;
    private final GameSettingRedisUtil gameSettingRedisUtil;
    private final UserStockPriceRedisUtil userStockPriceRedisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NextTradeDayVO nextTradeDay(NextTradeDayDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (!StringUtils.hasText(dto.getCurrentGameDate())) {
            throw new BusinessException("currentGameDate 不能为空");
        }

        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserInfo userInfo = userInfoMapper.selectByUserId(dto.getUserId());
        if (userInfo == null) {
            throw new BusinessException("用户尚未创建游戏存档");
        }

        LocalDate nextDate = parseDate(dto.getCurrentGameDate().trim()).plusDays(1);
        while (isWeekend(nextDate)) {
            nextDate = nextDate.plusDays(1);
        }
        String newDateStr = nextDate.format(DATE_FMT);

        userInfoMapper.updateGameDate(dto.getUserId(), newDateStr);
        userInfoMapper.updateGameTime(dto.getUserId(), OPEN_TIME);
        gameSettingRedisUtil.updateUserInfoDate(dto.getUserId(), newDateStr);
        gameSettingRedisUtil.updateUserInfoTime(dto.getUserId(), OPEN_TIME);

        // 新交易日开盘跳空（-3%~+3%），仅此处执行一次；盘中波动见 queryAndUpdateStockPrice
        applyOpeningGapForAllStocks(dto.getUserId());

        NextTradeDayVO vo = new NextTradeDayVO();
        vo.setGameDate(newDateStr);
        vo.setGameTime(OPEN_TIME);
        return vo;
    }

    /**
     * 全量股票开盘跳空：open_price = last_trade_price × (1 + [-3%, +3%])。
     * 写入 MySQL open_price，并以开盘价作为 Redis 当日首笔盘中价。
     */
    private void applyOpeningGapForAllStocks(Long userId) {
        List<UserStockPrice> stocks = userStockPriceMapper.selectByUserId(userId);
        if (stocks.isEmpty()) {
            throw new BusinessException("用户股票存档为空，请先完成游戏设置");
        }

        List<UserStockPrice> openUpdates = new ArrayList<>();
        Map<String, BigDecimal> redisOpenPrices = new HashMap<>();

        for (UserStockPrice stock : stocks) {
            BigDecimal lastClose = stock.getLastTradePrice();
            BigDecimal openPrice = StockPriceCalcUtil.calcOpeningGapPrice(lastClose);

            UserStockPrice row = new UserStockPrice();
            row.setUserId(userId);
            row.setStockCode(stock.getStockCode());
            row.setOpenPrice(openPrice);
            openUpdates.add(row);

            redisOpenPrices.put(stock.getStockCode(), openPrice);
        }

        userStockPriceMapper.batchUpdateOpenPrice(userId, openUpdates);
        userStockPriceRedisUtil.saveAllPrices(userId, redisOpenPrices);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException("currentGameDate 格式应为 yyyy-MM-dd");
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}

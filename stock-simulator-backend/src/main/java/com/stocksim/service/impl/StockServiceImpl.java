package com.stocksim.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksim.common.BusinessException;
import com.stocksim.constant.StockRandomEventPool;
import com.stocksim.dto.QueryAndUpdateStockPriceDTO;
import com.stocksim.entity.StockEvent;
import com.stocksim.entity.StockInfo;
import com.stocksim.entity.User;
import com.stocksim.entity.UserStockPrice;
import com.stocksim.mapper.StockInfoMapper;
import com.stocksim.mapper.UserInfoMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserStockPriceMapper;
import com.stocksim.service.StockEventService;
import com.stocksim.service.StockService;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.util.StockPriceCalcUtil;
import com.stocksim.util.UserStockPriceRedisUtil;
import com.stocksim.vo.StockBasicVO;
import com.stocksim.vo.StockEventInfoVO;
import com.stocksim.vo.StockHistoryVO;
import com.stocksim.vo.StockPriceItemVO;
import com.stocksim.vo.StockPriceTickVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 0);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final StockInfoMapper stockInfoMapper;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final UserStockPriceMapper userStockPriceMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserStockPriceRedisUtil userStockPriceRedisUtil;
    private final GameSettingRedisUtil gameSettingRedisUtil;
    private final StockEventService stockEventService;

    @Override
    public List<String> listIndustries() {
        return stockInfoMapper.selectDistinctIndustries();
    }

    @Override
    public List<StockBasicVO> listByIndustry(String industry) {
        return stockInfoMapper.selectByIndustry(industry).stream().map(s -> {
            StockBasicVO vo = new StockBasicVO();
            vo.setStockCode(s.getStockCode());
            vo.setStockName(s.getStockName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public StockHistoryVO getHistory(String stockCode) {
        StockInfo info = stockInfoMapper.selectByCode(stockCode);
        if (info == null) {
            throw new BusinessException("股票不存在");
        }
        try {
            List<BigDecimal> prices = objectMapper.readValue(
                    info.getHistoryPrices(), new TypeReference<List<BigDecimal>>() {});
            StockHistoryVO vo = new StockHistoryVO();
            vo.setStockCode(info.getStockCode());
            vo.setStockName(info.getStockName());
            vo.setPrices(prices);
            return vo;
        } catch (Exception e) {
            throw new BusinessException("历史价格数据解析失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockPriceTickVO queryAndUpdateStockPrice(QueryAndUpdateStockPriceDTO dto) {
        validateTickRequest(dto);
        Long userId = dto.getUserId();
        requireUser(userId);

        LocalTime currentTime = parseGameTime(dto.getGameTime().trim());
        String gameDate = dto.getGameDate().trim();

        List<UserStockPrice> dbStocks = userStockPriceMapper.selectByUserId(userId);
        if (dbStocks.isEmpty()) {
            throw new BusinessException("用户尚未初始化股票存档，请先完成游戏设置");
        }

        Map<String, String> redisPrices = userStockPriceRedisUtil.getAllPrices(userId);
        if (redisPrices.isEmpty()) {
            loadRedisFromDatabase(userId, dbStocks);
            redisPrices = userStockPriceRedisUtil.getAllPrices(userId);
        }

        if (!currentTime.isBefore(MARKET_CLOSE)) {
            return buildTickResponse(
                    gameDate, formatTime(MARKET_CLOSE), dbStocks, redisPrices, true, false, null, null);
        }

        // ① 全部股票先执行日常 ±3% 波动
        Map<String, BigDecimal> updated = new HashMap<>();
        for (UserStockPrice stock : dbStocks) {
            BigDecimal base = readPrice(redisPrices, stock.getStockCode(), stock.getLastTradePrice());
            BigDecimal next = StockPriceCalcUtil.calcIntradayFluctuation(base);
            updated.put(stock.getStockCode(), next);
            userStockPriceRedisUtil.savePrice(userId, stock.getStockCode(), next);
        }

        LocalTime newTime = currentTime.plusMinutes(5);
        boolean isTradingEnd = !newTime.isBefore(MARKET_CLOSE);
        if (isTradingEnd) {
            newTime = MARKET_CLOSE;
        }
        String newTimeStr = formatTime(newTime);

        // ② 8% 概率触发随机事件，叠加到单只股票
        boolean eventOccur = false;
        String eventMsg = null;
        StockEventInfoVO eventInfo = null;
        EventTriggerResult triggerResult = tryTriggerRandomEvent(userId, gameDate, newTimeStr, dbStocks, updated);
        if (triggerResult != null) {
            eventOccur = true;
            eventMsg = triggerResult.eventMsg();
            eventInfo = triggerResult.eventInfo();
        }

        if (isTradingEnd) {
            persistClosePricesToDb(userId, dbStocks, updated);
        }

        syncGameTime(userId, newTimeStr);

        return buildTickResponse(
                gameDate, newTimeStr, dbStocks, toStringMap(updated), isTradingEnd, eventOccur, eventMsg, eventInfo);
    }

    private record EventTriggerResult(String eventMsg, StockEventInfoVO eventInfo) {
    }

    /**
     * 随机事件：先完成日常波动后，对单只股票叠加事件涨跌幅，并落库 MySQL + Redis List。
     *
     * @return 事件结果；未触发返回 null
     */
    private EventTriggerResult tryTriggerRandomEvent(Long userId,
                                                     String gameDate,
                                                     String gameTime,
                                                     List<UserStockPrice> dbStocks,
                                                     Map<String, BigDecimal> updated) {
        if (ThreadLocalRandom.current().nextDouble() >= StockRandomEventPool.TRIGGER_PROBABILITY) {
            return null;
        }

        int idx = ThreadLocalRandom.current().nextInt(dbStocks.size());
        UserStockPrice target = dbStocks.get(idx);
        String stockCode = target.getStockCode();

        StockRandomEventPool.EventTemplate template = StockRandomEventPool.pickRandomTemplate();
        BigDecimal changeRate = StockRandomEventPool.randomChangeRate(template.isBullish());

        BigDecimal afterDaily = updated.get(stockCode);
        BigDecimal afterEvent = StockPriceCalcUtil.applyEventShock(afterDaily, changeRate);
        updated.put(stockCode, afterEvent);
        userStockPriceRedisUtil.savePrice(userId, stockCode, afterEvent);

        String content = StockRandomEventPool.buildContent(
                template, target.getStockName(), stockCode);

        StockEvent event = new StockEvent();
        event.setUserId(userId);
        event.setStockCode(stockCode);
        event.setEventTitle(template.getTitle());
        event.setEventContent(content);
        event.setChangeRate(changeRate);
        event.setGameDate(gameDate);
        event.setGameTime(gameTime);
        event.setCreateTime(LocalDateTime.now());
        stockEventService.saveEvent(event);

        StockEventInfoVO info = new StockEventInfoVO();
        info.setEventTitle(template.getTitle());
        info.setEventContent(content);
        info.setStockCode(stockCode);
        info.setStockName(target.getStockName());
        info.setChangeRate(changeRate);

        BigDecimal pct = changeRate.multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        String sign = pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        String tag = template.isBullish() ? "利好" : "利空";
        String msg = String.format("【%s】%s %s：%s，股价变动 %s%s%%",
                tag, stockCode, target.getStockName(), template.getTitle(), sign, pct.toPlainString());
        return new EventTriggerResult(msg, info);
    }

    private void validateTickRequest(QueryAndUpdateStockPriceDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (!StringUtils.hasText(dto.getGameTime())) {
            throw new BusinessException("gameTime 不能为空");
        }
        if (!StringUtils.hasText(dto.getGameDate())) {
            throw new BusinessException("gameDate 不能为空");
        }
    }

    private void requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
    }

    private LocalTime parseGameTime(String gameTime) {
        try {
            return LocalTime.parse(gameTime, TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException("gameTime 格式应为 HH:mm");
        }
    }

    private String formatTime(LocalTime time) {
        return time.format(TIME_FMT);
    }

    private void loadRedisFromDatabase(Long userId, List<UserStockPrice> dbStocks) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (UserStockPrice s : dbStocks) {
            map.put(s.getStockCode(), s.getLastTradePrice());
        }
        userStockPriceRedisUtil.saveAllPrices(userId, map);
    }

    private BigDecimal readPrice(Map<String, String> redis, String code, BigDecimal fallback) {
        String raw = redis.get(code);
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        return new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
    }

    private void persistClosePricesToDb(Long userId, List<UserStockPrice> dbStocks, Map<String, BigDecimal> prices) {
        List<UserStockPrice> batch = new ArrayList<>();
        for (UserStockPrice stock : dbStocks) {
            BigDecimal close = prices.get(stock.getStockCode());
            if (close == null) {
                continue;
            }
            UserStockPrice row = new UserStockPrice();
            row.setUserId(userId);
            row.setStockCode(stock.getStockCode());
            row.setLastTradePrice(close);
            batch.add(row);
        }
        if (!batch.isEmpty()) {
            userStockPriceMapper.batchUpdateLastTradePrice(userId, batch);
        }
    }

    private void syncGameTime(Long userId, String gameTime) {
        userInfoMapper.updateGameTime(userId, gameTime);
        gameSettingRedisUtil.updateUserInfoTime(userId, gameTime);
    }

    private Map<String, String> toStringMap(Map<String, BigDecimal> prices) {
        Map<String, String> map = new HashMap<>();
        prices.forEach((k, v) -> map.put(k, v.toPlainString()));
        return map;
    }

    private StockPriceTickVO buildTickResponse(String gameDate,
                                               String gameTime,
                                               List<UserStockPrice> dbStocks,
                                               Map<String, String> priceMap,
                                               boolean isTradingEnd,
                                               boolean eventOccur,
                                               String eventMsg,
                                               StockEventInfoVO eventInfo) {
        List<StockPriceItemVO> items = new ArrayList<>();
        for (UserStockPrice stock : dbStocks) {
            StockPriceItemVO item = new StockPriceItemVO();
            item.setStockCode(stock.getStockCode());
            item.setStockName(stock.getStockName());
            item.setLastTradePrice(readPrice(priceMap, stock.getStockCode(), stock.getLastTradePrice()));
            items.add(item);
        }
        StockPriceTickVO vo = new StockPriceTickVO();
        vo.setGameDate(gameDate);
        vo.setGameTime(gameTime);
        vo.setIsTradingEnd(isTradingEnd);
        vo.setStocks(items);
        vo.setEventOccur(eventOccur);
        vo.setEventMsg(eventMsg);
        vo.setEventInfo(eventInfo);
        return vo;
    }
}

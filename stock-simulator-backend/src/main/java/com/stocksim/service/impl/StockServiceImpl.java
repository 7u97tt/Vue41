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
import com.stocksim.service.GameClockService;
import com.stocksim.service.StockEventService;
import com.stocksim.service.StockService;
import com.stocksim.vo.GameClockVO;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.util.StockPriceCalcUtil;
import com.stocksim.util.UserStockPriceRedisUtil;
import com.stocksim.vo.StockBasicVO;
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
    private final GameClockService gameClockService;

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

        GameClockVO clock = gameClockService.resolve(userId);
        String gameDate = StringUtils.hasText(clock.getGameDate())
                ? clock.getGameDate()
                : dto.getGameDate().trim();
        String tickTimeStr = resolveTickGameTime(clock.getGameTime(), dto.getGameTime());
        LocalTime currentTime = parseGameTime(tickTimeStr);

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
                    gameDate, formatTime(MARKET_CLOSE), dbStocks, redisPrices, true, null);
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
        EventTriggerResult triggerResult = tryTriggerRandomEvent(userId, gameDate, newTimeStr, dbStocks, updated);

        if (isTradingEnd) {
            persistClosePricesToDb(userId, dbStocks, updated);
        }

        syncGameTime(userId, newTimeStr);

        return buildTickResponse(
                gameDate, newTimeStr, dbStocks, toStringMap(updated), isTradingEnd, triggerResult);
    }

    private record EventTriggerResult(
            String eventTitle,
            String eventContent,
            String affectedStockCode,
            BigDecimal changeRate
    ) {
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

        return new EventTriggerResult(
                template.getTitle(),
                content,
                stockCode,
                changeRate
        );
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

    /** 服务端时钟为准；仅允许前端 11:30→13:00 午休跳转 */
    private static String resolveTickGameTime(String serverTime, String clientTime) {
        if (!StringUtils.hasText(clientTime)) {
            return StringUtils.hasText(serverTime) ? serverTime.trim() : "09:30";
        }
        String client = clientTime.trim();
        if (!StringUtils.hasText(serverTime)) {
            return client;
        }
        String server = serverTime.trim();
        if ("13:00".equals(client) && "11:30".equals(server)) {
            return client;
        }
        return server;
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
                                               EventTriggerResult event) {
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
        if (event != null) {
            vo.setEventOccur(true);
            vo.setEventTitle(event.eventTitle());
            vo.setEventContent(event.eventContent());
            vo.setAffectedStockCode(event.affectedStockCode());
            vo.setChangeRate(event.changeRate());
        } else {
            vo.setEventOccur(false);
        }
        return vo;
    }
}

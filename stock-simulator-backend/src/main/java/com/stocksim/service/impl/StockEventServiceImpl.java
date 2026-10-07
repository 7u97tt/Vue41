package com.stocksim.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksim.common.BusinessException;
import com.stocksim.entity.StockEvent;
import com.stocksim.mapper.StockEventMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.service.StockEventService;
import com.stocksim.util.UserStockEventRedisUtil;
import com.stocksim.vo.StockEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockEventServiceImpl implements StockEventService {

    private static final int RECENT_LIMIT = 20;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StockEventMapper stockEventMapper;
    private final UserMapper userMapper;
    private final UserStockEventRedisUtil userStockEventRedisUtil;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEvent(StockEvent event) {
        stockEventMapper.insert(event);
        try {
            StockEventVO vo = toVo(event);
            String json = objectMapper.writeValueAsString(vo);
            userStockEventRedisUtil.pushEventJson(event.getUserId(), json);
        } catch (Exception e) {
            throw new BusinessException("事件写入 Redis 失败");
        }
    }

    @Override
    public List<StockEventVO> getUserRecentEvents(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> jsonList = userStockEventRedisUtil.listRecentEventJson(userId);
        if (!jsonList.isEmpty()) {
            List<StockEventVO> fromRedis = new ArrayList<>();
            for (String json : jsonList) {
                if (!StringUtils.hasText(json)) {
                    continue;
                }
                try {
                    fromRedis.add(objectMapper.readValue(json, StockEventVO.class));
                } catch (Exception ignored) {
                    /* 单条损坏则跳过 */
                }
            }
            if (!fromRedis.isEmpty()) {
                return fromRedis;
            }
        }

        return stockEventMapper.selectRecentByUserId(userId, RECENT_LIMIT).stream()
                .map(this::toVo)
                .toList();
    }

    private StockEventVO toVo(StockEvent event) {
        StockEventVO vo = new StockEventVO();
        vo.setId(event.getId());
        vo.setStockCode(event.getStockCode());
        vo.setEventTitle(event.getEventTitle());
        vo.setEventContent(event.getEventContent());
        vo.setChangeRate(event.getChangeRate());
        vo.setGameDate(event.getGameDate());
        vo.setGameTime(event.getGameTime());
        if (event.getCreateTime() != null) {
            vo.setCreateTime(event.getCreateTime().format(DT_FMT));
        }
        return vo;
    }
}

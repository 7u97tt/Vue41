package com.stocksim.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksim.common.BusinessException;
import com.stocksim.constant.GameStockCatalog;
import com.stocksim.entity.UserInfo;
import com.stocksim.entity.UserOperateLog;
import com.stocksim.mapper.UserInfoMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.mapper.UserOperateLogMapper;
import com.stocksim.service.UserOperateLogService;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.util.UserOperateLogRedisUtil;
import com.stocksim.vo.OperateLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserOperateLogServiceImpl implements UserOperateLogService {

    private static final int MYSQL_FALLBACK_LIMIT = 500;
    private static final int FUND_IN = 1;
    private static final int FUND_OUT = -1;
    private static final int FUND_NONE = 0;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserOperateLogMapper userOperateLogMapper;
    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final GameSettingRedisUtil gameSettingRedisUtil;
    private final UserOperateLogRedisUtil userOperateLogRedisUtil;
    private final ObjectMapper objectMapper;

    @Override
    public List<OperateLogVO> getOperateLogs(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> jsonList = userOperateLogRedisUtil.listRecentLogJson(userId);
        if (!jsonList.isEmpty()) {
            List<OperateLogVO> fromRedis = new ArrayList<>();
            for (String json : jsonList) {
                if (!StringUtils.hasText(json)) {
                    continue;
                }
                try {
                    fromRedis.add(objectMapper.readValue(json, OperateLogVO.class));
                } catch (Exception ignored) {
                    /* skip corrupt entry */
                }
            }
            if (!fromRedis.isEmpty()) {
                return fromRedis;
            }
        }

        return userOperateLogMapper.selectByUserId(userId, MYSQL_FALLBACK_LIMIT).stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBuyStock(Long userId,
                               String stockCode,
                               int buyCount,
                               BigDecimal tradeTotal,
                               BigDecimal handFee,
                               BigDecimal actualDebit,
                               BigDecimal afterMoney) {
        String label = stockLabel(stockCode);
        String remark = String.format("买入【%s】%d股，含手续费", label, buyCount);
        appendLog(userId, "买入股票", stockCode, absMoney(actualDebit), FUND_OUT, afterMoney, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSellStock(Long userId,
                                String stockCode,
                                int sellCount,
                                BigDecimal tradeAmount,
                                BigDecimal handFee,
                                BigDecimal netReceived,
                                BigDecimal afterMoney) {
        String label = stockLabel(stockCode);
        String remark = String.format("卖出【%s】%d股，扣除手续费", label, sellCount);
        appendLog(userId, "卖出股票", stockCode, absMoney(netReceived), FUND_IN, afterMoney, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordShortBorrow(Long userId, String stockCode, int borrowCount, BigDecimal afterMoney) {
        String label = stockLabel(stockCode);
        String remark = String.format("融券借入【%s】%d股，无资金变动", label, borrowCount);
        appendLog(userId, "融券借券", stockCode, BigDecimal.ZERO.setScale(2), FUND_NONE, afterMoney, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordShortSell(Long userId,
                                String stockCode,
                                int sellCount,
                                BigDecimal tradeAmount,
                                BigDecimal handFee,
                                BigDecimal netReceived,
                                BigDecimal afterMoney) {
        String label = stockLabel(stockCode);
        String remark = String.format("融券卖出【%s】%d股，扣除手续费", label, sellCount);
        appendLog(userId, "融券卖出", stockCode, absMoney(netReceived), FUND_IN, afterMoney, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordShortReturn(Long userId,
                                  String stockCode,
                                  int returnCount,
                                  BigDecimal totalCost,
                                  BigDecimal handFee,
                                  BigDecimal actualDebit,
                                  BigDecimal afterMoney) {
        String label = stockLabel(stockCode);
        String remark = String.format("归还平仓【%s】%d股，含平仓手续费", label, returnCount);
        appendLog(userId, "做空平仓归还", stockCode, absMoney(actualDebit), FUND_OUT, afterMoney, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordShortDailyBorrowFee(Long userId,
                                          String stockCode,
                                          BigDecimal borrowBaseTotal,
                                          BigDecimal dailyFee,
                                          BigDecimal afterMoney) {
        BigDecimal base = borrowBaseTotal == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : borrowBaseTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = absMoney(dailyFee);
        String remark = String.format(
                "融券持仓日手续费，借券基准总额%s，扣除当日5%%手续费%s",
                base.toPlainString(),
                fee.toPlainString());
        appendLog(userId, "融券日手续费", stockCode, fee, FUND_OUT, afterMoney, remark);
    }

    private void appendLog(Long userId,
                           String operateType,
                           String stockCode,
                           BigDecimal changeMoney,
                           int fundDirection,
                           BigDecimal afterMoney,
                           String remark) {
        GameClock clock = resolveGameClock(userId);
        UserOperateLog row = new UserOperateLog();
        row.setUserId(userId);
        row.setOperateType(operateType);
        row.setStockCode(stockCode);
        row.setChangeMoney(changeMoney.setScale(2, RoundingMode.HALF_UP));
        row.setFundDirection(fundDirection);
        row.setAfterMoney(afterMoney.setScale(2, RoundingMode.HALF_UP));
        row.setGameDate(clock.gameDate());
        row.setGameTime(clock.gameTime());
        row.setRemark(remark);
        row.setCreateTime(LocalDateTime.now());
        userOperateLogMapper.insert(row);
        try {
            OperateLogVO vo = toVo(row);
            String json = objectMapper.writeValueAsString(vo);
            userOperateLogRedisUtil.pushLogJson(userId, json);
        } catch (Exception e) {
            throw new BusinessException("操作日志写入 Redis 失败");
        }
    }

    private static BigDecimal absMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.abs().setScale(2, RoundingMode.HALF_UP);
    }

    private static String stockLabel(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            return "";
        }
        String code = stockCode.trim();
        return GameStockCatalog.all().stream()
                .filter(item -> item.getStockCode().equals(code))
                .findFirst()
                .map(item -> item.getStockName() + item.getStockCode())
                .orElse(code);
    }

    private GameClock resolveGameClock(Long userId) {
        String date = gameSettingRedisUtil.getInfoField(userId, "date");
        String time = gameSettingRedisUtil.getInfoField(userId, "time");
        if (StringUtils.hasText(date) && StringUtils.hasText(time)) {
            return new GameClock(date.trim(), time.trim());
        }
        UserInfo info = userInfoMapper.selectByUserId(userId);
        if (info != null && StringUtils.hasText(info.getGameDate()) && StringUtils.hasText(info.getGameTime())) {
            return new GameClock(info.getGameDate().trim(), info.getGameTime().trim());
        }
        return new GameClock("", "");
    }

    private OperateLogVO toVo(UserOperateLog log) {
        OperateLogVO vo = new OperateLogVO();
        vo.setId(log.getId());
        vo.setOperateType(log.getOperateType());
        vo.setStockCode(log.getStockCode());
        vo.setChangeMoney(log.getChangeMoney());
        vo.setFundDirection(log.getFundDirection());
        vo.setAfterMoney(log.getAfterMoney());
        vo.setGameDate(log.getGameDate());
        vo.setGameTime(log.getGameTime());
        vo.setRemark(log.getRemark());
        if (log.getCreateTime() != null) {
            vo.setCreateTime(log.getCreateTime().format(DT_FMT));
        }
        return vo;
    }

    private record GameClock(String gameDate, String gameTime) {
    }
}

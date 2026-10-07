package com.stocksim.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机事件池：利空 -10%~-25%，利好 +10%~+25%。
 */
public final class StockRandomEventPool {

    public static final double TRIGGER_PROBABILITY = 0.08;

    @Getter
    @AllArgsConstructor
    public static class EventTemplate {
        private final String title;
        private final String contentPattern;
        private final boolean bullish;
    }

    private static final List<EventTemplate> POOL = List.of(
            new EventTemplate("财报造假曝光", "%s（%s）被曝财务数据严重造假，市场信心崩塌。", false),
            new EventTemplate("CEO被逮捕", "%s（%s）首席执行官因涉嫌违法被逮捕，股价遭遇重挫。", false),
            new EventTemplate("爆发战争", "地缘政治紧张，%s（%s）所在产业链受到冲击。", false),
            new EventTemplate("拿到超大订单", "%s（%s）宣布获得巨额长期订单，业绩预期大幅上调。", true),
            new EventTemplate("重大技术突破", "%s（%s）发布革命性技术成果，获机构集中看好。", true),
            new EventTemplate("原材料暴涨", "上游原材料价格飙升，%s（%s）成本压力骤增，盈利预期下调。", false)
    );

    private StockRandomEventPool() {
    }

    public static EventTemplate pickRandomTemplate() {
        int idx = ThreadLocalRandom.current().nextInt(POOL.size());
        return POOL.get(idx);
    }

    public static BigDecimal randomChangeRate(boolean bullish) {
        double rate;
        if (bullish) {
            rate = ThreadLocalRandom.current().nextDouble(0.10, 0.25);
        } else {
            rate = ThreadLocalRandom.current().nextDouble(-0.25, -0.10);
        }
        return BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP);
    }

    public static String buildContent(EventTemplate template, String stockName, String stockCode) {
        return String.format(template.getContentPattern(), stockName, stockCode);
    }
}

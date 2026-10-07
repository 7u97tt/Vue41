package com.stocksim.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 股价计算工具（BigDecimal，避免浮点误差）。
 */
public final class StockPriceCalcUtil {

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");

    private StockPriceCalcUtil() {
    }

    /**
     * 开盘跳空：open = lastClose × (1 + r)，r ∈ [-3%, +3%]。
     * 仅在新交易日 nextTradeDay 开盘时执行一次；与盘中 tick 波动无关。
     */
    public static BigDecimal calcOpeningGapPrice(BigDecimal lastClosePrice) {
        if (lastClosePrice == null) {
            throw new IllegalArgumentException("lastClosePrice 不能为空");
        }
        double rate = ThreadLocalRandom.current().nextDouble(-0.03, 0.03);
        BigDecimal open = lastClosePrice
                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(rate)))
                .setScale(2, RoundingMode.HALF_UP);
        if (open.compareTo(MIN_PRICE) < 0) {
            return MIN_PRICE;
        }
        return open;
    }

    /**
     * 盘中波动：current × (1 + r)，r ∈ [-3%, +3%]（queryAndUpdateStockPrice 每 tick 调用）。
     */
    public static BigDecimal calcIntradayFluctuation(BigDecimal currentPrice) {
        if (currentPrice == null) {
            throw new IllegalArgumentException("currentPrice 不能为空");
        }
        double rate = ThreadLocalRandom.current().nextDouble(-0.03, 0.03);
        BigDecimal next = currentPrice
                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(rate)))
                .setScale(2, RoundingMode.HALF_UP);
        if (next.compareTo(MIN_PRICE) < 0) {
            return MIN_PRICE;
        }
        return next;
    }

    /** 叠加随机事件涨跌幅：price × (1 + changeRate) */
    public static BigDecimal applyEventShock(BigDecimal currentPrice, BigDecimal changeRate) {
        if (currentPrice == null || changeRate == null) {
            throw new IllegalArgumentException("价格与涨跌幅不能为空");
        }
        BigDecimal next = currentPrice
                .multiply(BigDecimal.ONE.add(changeRate))
                .setScale(2, RoundingMode.HALF_UP);
        if (next.compareTo(MIN_PRICE) < 0) {
            return MIN_PRICE;
        }
        return next;
    }
}

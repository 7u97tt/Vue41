package com.stocksim.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 游戏固定股票清单（每个用户存档初始化时复制一份到 user_stock_price）。
 */
public final class GameStockCatalog {

    @Getter
    @AllArgsConstructor
    public static class StockItem {
        private final String stockName;
        private final String stockCode;
        private final String industry;
    }

    private static final List<StockItem> ITEMS = List.of(
            new StockItem("噢喵洗涤", "601001", "日用日化"),
            new StockItem("象印纸业", "601002", "日用日化"),
            new StockItem("哼哒地产", "602001", "房地产"),
            new StockItem("星连航空", "603001", "交通运输"),
            new StockItem("瞬风快递", "603002", "交通运输"),
            new StockItem("木原养殖", "604001", "农林渔牧"),
            new StockItem("光头林业", "604002", "农林渔牧"),
            new StockItem("手钢钢铁", "605001", "冶金矿工"),
            new StockItem("善喜煤炭", "605002", "冶金矿工"),
            new StockItem("钟石化石油", "605003", "石油化工"),
            new StockItem("钟石由石油", "605004", "石油化工"),
            new StockItem("妙妙电子", "606001", "电子信息"),
            new StockItem("大米信息", "606002", "电子信息"),
            new StockItem("保利工业", "607001", "机械机电"),
            new StockItem("红漆机械", "607002", "机械机电"),
            new StockItem("汪汪食品", "608001", "食品玩具"),
            new StockItem("基米玩具", "608002", "食品玩具"),
            new StockItem("康帅傅饮品", "608003", "食品玩具"),
            new StockItem("呦一裤服装", "609001", "服装纺织"),
            new StockItem("风柱纺织", "609002", "服装纺织"),
            new StockItem("杨光保险", "610001", "专业服务"),
            new StockItem("专业律所", "610002", "专业服务"),
            new StockItem("摩根银行", "610003", "专业服务"),
            new StockItem("妙妙汽车", "611001", "汽车制造"),
            new StockItem("大米汽车", "611002", "汽车制造"),
            new StockItem("晨咣文具", "612001", "办公文教"),
            new StockItem("德利文具", "612002", "办公文教"),
            new StockItem("怡家家具", "613001", "家具用品"),
            new StockItem("美白勺电器", "613002", "家具用品"),
            new StockItem("嗨耳兄弟电器", "613003", "家具用品")
    );

    public static List<StockItem> all() {
        return Collections.unmodifiableList(ITEMS);
    }

    private GameStockCatalog() {
    }
}

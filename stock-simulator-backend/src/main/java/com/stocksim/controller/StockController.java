package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.dto.BuyStockDTO;
import com.stocksim.dto.QueryAndUpdateStockPriceDTO;
import com.stocksim.dto.SellStockDTO;
import com.stocksim.service.BuyStockService;
import com.stocksim.entity.UserStockPrice;
import com.stocksim.service.SellStockService;
import com.stocksim.service.StockService;
import com.stocksim.service.UserStockPriceService;
import com.stocksim.vo.SellStockVO;
import com.stocksim.vo.StockBasicVO;
import com.stocksim.vo.StockHistoryVO;
import com.stocksim.vo.StockPriceTickVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前后端约定（说明用，不参与业务逻辑）：
 * <ol>
 *   <li>queryAndUpdateStockPrice：game 页进入即调一次，之后每 2 秒调一次；isTradingEnd=true 时停止轮询。</li>
 *   <li>现实 2 秒 = 游戏内 +5 分钟；收盘 15:00 落库 MySQL 并返回 isTradingEnd=true。</li>
 *   <li>buyStock：入参 userId、stockCode、buyCount、handFeeRate=0.003；扣款=成交总额+0.3%手续费；
 *       成功后关闭弹窗，由 2 秒轮询 getMyAsset 刷新资产与持仓。</li>
 *   <li>sellStock：入参 userId、stockCode、sellCount；手续费固定 0.3%；到账=成交金额-手续费；
 *       返回 sellCount、tradeAmount、handFee、netReceived。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final UserStockPriceService userStockPriceService;
    private final BuyStockService buyStockService;
    private final SellStockService sellStockService;

    @GetMapping("/industries")
    public Result<List<String>> industries() {
        return Result.ok(stockService.listIndustries());
    }

    @GetMapping("/list")
    public Result<List<StockBasicVO>> listByIndustry(@RequestParam String industry) {
        return Result.ok(stockService.listByIndustry(industry));
    }

    @GetMapping("/history/{code}")
    public Result<StockHistoryVO> history(@PathVariable("code") String code) {
        return Result.ok(stockService.getHistory(code));
    }

    @GetMapping("/getUserAllStock")
    public Result<List<UserStockPrice>> getUserAllStock(@RequestParam Long userId) {
        return Result.ok(userStockPriceService.getUserAllStock(userId));
    }

    @PutMapping("/updateStockPrice")
    public Result<Void> updateStockPrice(@RequestBody UserStockPrice userStockPrice) {
        userStockPriceService.updateStockPrice(
                userStockPrice.getUserId(),
                userStockPrice.getStockCode(),
                userStockPrice.getLastTradePrice()
        );
        return Result.ok();
    }

    @PutMapping("/queryAndUpdateStockPrice")
    public Result<StockPriceTickVO> queryAndUpdateStockPrice(@RequestBody QueryAndUpdateStockPriceDTO dto) {
        return Result.ok(stockService.queryAndUpdateStockPrice(dto));
    }

    @PostMapping("/buyStock")
    public Result<Void> buyStock(@RequestBody BuyStockDTO dto) {
        buyStockService.buyStock(dto);
        return Result.ok();
    }

    @PostMapping("/sellStock")
    public Result<SellStockVO> sellStock(@RequestBody SellStockDTO dto) {
        return Result.ok(sellStockService.sellStock(dto));
    }
}

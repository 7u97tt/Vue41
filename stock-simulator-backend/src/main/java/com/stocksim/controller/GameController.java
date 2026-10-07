package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.dto.NextTradeDayDTO;
import com.stocksim.service.GameService;
import com.stocksim.vo.NextTradeDayVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前后端约定（说明用，不参与业务逻辑）：
 * <ol>
 *   <li>game 页初始游戏日期固定 2020-01-02；进入页立即调用 queryAndUpdateStockPrice，之后每现实 2 秒调用一次。</li>
 *   <li>后端无定时任务，时间推进由前端轮询驱动。</li>
 *   <li>isTradingEnd=true 时前端停止 2 秒轮询，等待用户点击【下一个交易日】。</li>
 *   <li>nextTradeDay 成功后用返回的新日期与时间，重新启动 2 秒轮询。</li>
 *   <li>盘中价仅存 Redis；15:00 收盘后写入 user_stock_price.last_trade_price。</li>
 *   <li>nextTradeDay 在日期切换后执行开盘跳空：open_price = 昨收 × (1 ± 0~3%)，仅执行一次；
 *       盘中 queryAndUpdateStockPrice 为另一套 ±3% tick 波动。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/nextTradeDay")
    public Result<NextTradeDayVO> nextTradeDay(@RequestBody NextTradeDayDTO dto) {
        return Result.ok(gameService.nextTradeDay(dto));
    }
}

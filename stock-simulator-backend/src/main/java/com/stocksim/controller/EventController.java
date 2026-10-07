package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.service.StockEventService;
import com.stocksim.vo.StockEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final StockEventService stockEventService;

    /**
     * 优先读 Redis {userId}eventList，为空再查 MySQL stock_event。
     */
    @GetMapping("/getUserRecentEvent")
    public Result<List<StockEventVO>> getUserRecentEvent(@RequestParam Long userId) {
        return Result.ok(stockEventService.getUserRecentEvents(userId));
    }
}

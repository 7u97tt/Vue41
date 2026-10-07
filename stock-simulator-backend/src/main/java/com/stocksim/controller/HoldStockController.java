package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.dto.TradeRequest;
import com.stocksim.service.HoldStockService;
import com.stocksim.vo.HoldStockVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hold")
@RequiredArgsConstructor
public class HoldStockController {

    private final HoldStockService holdStockService;

    @GetMapping("/list")
    public Result<List<HoldStockVO>> list(@RequestParam(required = false) Long userId) {
        return Result.ok(holdStockService.listMyHolds(userId));
    }

    @PostMapping("/buy")
    public Result<List<HoldStockVO>> buy(@Valid @RequestBody TradeRequest request) {
        holdStockService.buy(request);
        return Result.ok(holdStockService.listMyHolds(request.getUserId()));
    }

    @PostMapping("/sell")
    public Result<List<HoldStockVO>> sell(@Valid @RequestBody TradeRequest request) {
        holdStockService.sell(request);
        return Result.ok(holdStockService.listMyHolds(request.getUserId()));
    }
}

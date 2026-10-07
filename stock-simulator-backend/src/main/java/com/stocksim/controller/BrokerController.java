package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.dto.BorrowStockDTO;
import com.stocksim.dto.ReturnStockDTO;
import com.stocksim.dto.ShortSellDTO;
import com.stocksim.service.BrokerService;
import com.stocksim.vo.UserShortInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/broker")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

    @PostMapping("/borrowStock")
    public Result<Void> borrowStock(@RequestBody BorrowStockDTO dto) {
        brokerService.borrowStock(dto);
        return Result.ok();
    }

    @PostMapping("/shortSell")
    public Result<Void> shortSell(@RequestBody ShortSellDTO dto) {
        brokerService.shortSell(dto);
        return Result.ok();
    }

    @PostMapping("/returnStock")
    public Result<Void> returnStock(@RequestBody ReturnStockDTO dto) {
        brokerService.returnStock(dto);
        return Result.ok();
    }

    @GetMapping("/getUserShortInfo")
    public Result<List<UserShortInfoVO>> getUserShortInfo(@RequestParam Long userId) {
        return Result.ok(brokerService.getUserShortInfo(userId));
    }

    @GetMapping("/getCanBorrow")
    public Result<Integer> getCanBorrow(@RequestParam String stockCode) {
        return Result.ok(brokerService.getCanBorrowCount(stockCode));
    }
}

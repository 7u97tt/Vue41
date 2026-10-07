package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.service.UserOperateLogService;
import com.stocksim.vo.OperateLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class OperateLogController {

    private final UserOperateLogService userOperateLogService;

    @GetMapping("/getOperateLog")
    public Result<List<OperateLogVO>> getOperateLog(@RequestParam Long userId) {
        return Result.ok(userOperateLogService.getOperateLogs(userId));
    }
}

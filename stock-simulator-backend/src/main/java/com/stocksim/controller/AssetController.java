package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.service.AssetService;
import com.stocksim.vo.MyAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/getMyAsset")
    public Result<MyAssetVO> getMyAsset(@RequestParam Long userId) {
        return Result.ok(assetService.getMyAsset(userId));
    }
}

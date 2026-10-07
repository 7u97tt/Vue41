package com.stocksim.service;

import com.stocksim.vo.MyAssetVO;

public interface AssetService {

    MyAssetVO getMyAsset(Long userId);
}

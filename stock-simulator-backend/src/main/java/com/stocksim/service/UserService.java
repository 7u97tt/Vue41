package com.stocksim.service;

import com.stocksim.dto.LoginDTO;
import com.stocksim.dto.RegisterDTO;
import com.stocksim.dto.SaveGameSettingDTO;
import com.stocksim.vo.LoginVO;

public interface UserService {

    Long register(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    /**
     * 保存难度与初始游戏设置；Redis key 为 userId + "Difficulty" / userId + "info"。
     */
    void saveGameSetting(SaveGameSettingDTO dto);

    /**
     * 是否已有游戏存档（以 userinfo 表为准）。
     */
    boolean checkHasGameArchive(Long userId);
}

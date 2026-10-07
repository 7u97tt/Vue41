package com.stocksim.service.impl;

import com.stocksim.common.BusinessException;
import com.stocksim.dto.LoginDTO;
import com.stocksim.dto.RegisterDTO;
import com.stocksim.dto.SaveGameSettingDTO;
import com.stocksim.entity.User;
import com.stocksim.entity.UserInfo;
import com.stocksim.mapper.UserInfoMapper;
import com.stocksim.mapper.UserMapper;
import com.stocksim.service.UserService;
import com.stocksim.service.UserStockPriceService;
import com.stocksim.util.GameSettingRedisUtil;
import com.stocksim.util.LoginTokenRedisUtil;
import com.stocksim.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String LOGIN_FAIL_MSG = "用户名或密码错误";
    private static final Set<String> ALLOWED_DIFFICULTY = Set.of("简单", "中等", "困难", "自定义");

    /** MySQL userinfo.difficulty */
    private static final Map<String, Integer> DIFFICULTY_TO_CODE = Map.of(
            "简单", 0,
            "中等", 1,
            "困难", 2,
            "自定义", 1
    );

    /** Redis {userId}Difficulty，示例 key "1Difficulty" */
    private static final Map<String, String> DIFFICULTY_TO_REDIS = Map.of(
            "简单", "0",
            "中等", "1",
            "困难", "2",
            "自定义", "custom"
    );

    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final LoginTokenRedisUtil loginTokenRedisUtil;
    private final GameSettingRedisUtil gameSettingRedisUtil;
    private final UserStockPriceService userStockPriceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO registerDTO) {
        if (registerDTO == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (!StringUtils.hasText(registerDTO.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(registerDTO.getPassword())
                || !StringUtils.hasText(registerDTO.getConfirmPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        String username = registerDTO.getUsername().trim();
        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new BusinessException("用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(registerDTO.getPassword());
        userMapper.insert(user);
        return user.getUserId();
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        if (loginDTO == null
                || !StringUtils.hasText(loginDTO.getUsername())
                || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new BusinessException(LOGIN_FAIL_MSG);
        }

        String username = loginDTO.getUsername().trim();
        User user = userMapper.selectByUsername(username);
        if (user == null || !loginDTO.getPassword().equals(user.getPassword())) {
            throw new BusinessException(LOGIN_FAIL_MSG);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        loginTokenRedisUtil.saveLoginToken(token, user.getUserId(), user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGameSetting(SaveGameSettingDTO dto) {
        validateSaveGameSetting(dto);

        Long userId = dto.getUserId();
        String username = dto.getUsername().trim();
        User user = userMapper.selectByUsername(username);
        if (user == null || !user.getUserId().equals(userId)) {
            throw new BusinessException("用户不存在或 userId 与用户名不匹配");
        }

        String difficultyLabel = dto.getDifficulty().trim();
        String redisDifficulty = DIFFICULTY_TO_REDIS.get(difficultyLabel);

        // Redis：key 示例 "1Difficulty"；hash 示例 "1info"
        gameSettingRedisUtil.saveDifficulty(userId, redisDifficulty);
        gameSettingRedisUtil.saveUserInfo(
                userId,
                dto.getMoney(),
                dto.getGameTime().trim(),
                dto.getGameDate().trim()
        );

        UserInfo existing = userInfoMapper.selectByUserId(userId);
        UserInfo record = new UserInfo();
        record.setUserId(userId);
        record.setUsername(username);
        record.setMoney(dto.getMoney());
        record.setGameTime(dto.getGameTime().trim());
        record.setGameDate(dto.getGameDate().trim());
        record.setDifficulty(DIFFICULTY_TO_CODE.get(difficultyLabel));

        if (existing == null) {
            userInfoMapper.insert(record);
        } else {
            userInfoMapper.updateGameData(record);
        }
        userStockPriceService.initUserStockArchive(userId);
    }

    @Override
    public boolean checkHasGameArchive(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        return userMapper.existsUserInfoByUserId(userId);
    }

    private void validateSaveGameSetting(SaveGameSettingDTO dto) {
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (dto.getUserId() == null) {
            throw new BusinessException("userId 不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException("username 不能为空");
        }
        if (!StringUtils.hasText(dto.getDifficulty()) || !ALLOWED_DIFFICULTY.contains(dto.getDifficulty().trim())) {
            throw new BusinessException("难度无效，应为：简单、中等、困难、自定义");
        }
        if (dto.getMoney() == null || dto.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("初始金钱必须大于 0");
        }
        if (!StringUtils.hasText(dto.getGameTime())) {
            throw new BusinessException("gameTime 不能为空");
        }
        if (!StringUtils.hasText(dto.getGameDate())) {
            throw new BusinessException("gameDate 不能为空");
        }
    }
}

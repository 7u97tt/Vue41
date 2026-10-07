package com.stocksim.controller;

import com.stocksim.common.Result;
import com.stocksim.dto.LoginDTO;
import com.stocksim.dto.RegisterDTO;
import com.stocksim.dto.SaveGameSettingDTO;
import com.stocksim.service.UserService;
import com.stocksim.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 前后端约定（说明用，不参与业务逻辑）：
 * <ol>
 *   <li>登录成功拿到 userId，调用 GET /api/user/checkHasGameArchive?userId=xxx；
 *       hasArchive=false → router.push('/home')；hasArchive=true → router.push('/game')。</li>
 *   <li>home 页调用 saveGameSetting 时必须传 userId（用于 Redis key 与 MySQL）。</li>
 *   <li>Redis key 规则：字符串 {@code {userId}Difficulty}（示例 {@code 1Difficulty}）；
 *       Hash {@code {userId}info}（示例 {@code 1info}，字段 money/time/date）。</li>
 *   <li>存档是否存在以 MySQL userinfo 为准；Redis 与库不一致时仍以数据库为准。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Map<String, Long>> register(@RequestBody RegisterDTO registerDTO) {
        Long userId = userService.register(registerDTO);
        return Result.success("注册成功", Map.of("user_id", userId));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }

    @GetMapping("/checkHasGameArchive")
    public Result<Map<String, Boolean>> checkHasGameArchive(@RequestParam Long userId) {
        boolean hasArchive = userService.checkHasGameArchive(userId);
        return Result.ok(Map.of("hasArchive", hasArchive));
    }

    @PostMapping("/saveGameSetting")
    public Result<Void> saveGameSetting(@RequestBody SaveGameSettingDTO saveGameSettingDTO) {
        userService.saveGameSetting(saveGameSettingDTO);
        return Result.ok();
    }
}

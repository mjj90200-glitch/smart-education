package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.dto.LoginRequest;
import com.smartedu.dto.LoginResponse;
import com.smartedu.dto.RegisterRequest;
import com.smartedu.entity.User;
import com.smartedu.service.UserService;
import com.smartedu.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * @author SmartEdu Team
 */
@Tag(name = "认证管理", description = "用户登录、注册、登出等认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.getUsername(), request.getPassword());
        return Result.success("登录成功", response);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        // TODO: 处理登出逻辑（如将 Token 加入黑名单）
        return Result.success();
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> getCurrentUserInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }

        // 从用户名解析用户 ID
        try {
            Long userId = Long.parseLong(userDetails.getUsername());
            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            // UserInfoVO 不包含 password 字段，自动过滤密码

            return Result.success(userInfoVO);
        } catch (NumberFormatException e) {
            return Result.error("用户 ID 格式错误");
        }
    }
}

package com.xinchang.management.auth.controller;

import com.xinchang.management.auth.service.AuthService;
import com.xinchang.management.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证登录控制器，提供用户登录、登出及当前用户信息查询接口
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证登录", description = "用户登录、登出及用户信息查询接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录接口，验证用户名和密码后返回token
     * @param body 包含用户名和密码的请求体
     * @return 包含token及用户信息的登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码进行身份验证，返回登录token")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return Result.ok(authService.login(username, password));
    }

    /**
     * 用户登出接口，清除当前登录会话
     * @return 操作结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "清除当前用户的登录会话")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "查询当前已登录用户的基本信息")
    public Result<Map<String, Object>> me() {
        return Result.ok(authService.getCurrentUser());
    }

    /**
     * 修改当前用户密码
     * @param body 包含旧密码和新密码的请求体
     * @return 操作结果
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码，需验证原密码正确")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }
}

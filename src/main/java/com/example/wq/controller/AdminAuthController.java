package com.example.wq.controller;

import com.example.wq.entity.AdminUser;
import com.example.wq.entity.Result;
import com.example.wq.service.AdminAuthService;
import com.example.wq.util.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台管理系统认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "后台管理认证", description = "后台管理系统登录和Token管理接口")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final JwtTokenUtil jwtTokenUtil;

    public AdminAuthController(AdminAuthService adminAuthService,
                               JwtTokenUtil jwtTokenUtil) {
        this.adminAuthService = adminAuthService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "通过用户名密码登录后台管理系统，返回JWT token和管理员信息")
    public Result<Map<String, Object>> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest servletRequest) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        log.info("管理员登录请求: username={}", username);

        try {
            Map<String, Object> response = adminAuthService.login(username, password, servletRequest);

            log.info("管理员登录成功: username={}, adminId={}",
                    username, ((Map<String, Object>) response.get("admin")).get("id"));

            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("管理员登录失败: username={}, error={}", username, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新管理员Token", description = "使用refreshToken获取新的accessToken")
    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Result.error(400, "refreshToken不能为空");
        }

        log.info("刷新管理员Token请求");

        try {
            Map<String, String> response = adminAuthService.refreshAccessToken(refreshToken);
            return Result.success("Token刷新成功", response);
        } catch (Exception e) {
            log.error("刷新管理员Token失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证Token
     */
    @PostMapping("/validate")
    @Operation(summary = "验证管理员Token", description = "验证JWT token是否有效")
    public Result<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {

        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return Result.error(400, "token不能为空");
        }

        boolean isValid = adminAuthService.validateToken(token);

        if (!isValid) {
            return Result.error(401, "Token无效或已过期");
        }

        String adminId = jwtTokenUtil.getUserIdFromToken(token);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String tokenType = jwtTokenUtil.getTokenTypeFromToken(token);

        return Result.success("Token有效", Map.of(
                "valid", true,
                "adminId", adminId,
                "username", username,
                "tokenType", tokenType
        ));
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前管理员信息", description = "根据Token获取当前登录管理员的完整信息")
    public Result<AdminUser> getCurrentAdminInfo(
            @Parameter(description = "Authorization header (Bearer token)", required = true)
            @RequestHeader("Authorization") String authorization) {

        String token = jwtTokenUtil.extractTokenFromHeader(authorization);
        if (token == null) {
            return Result.error(401, "无效的Authorization header格式");
        }

        if (!adminAuthService.validateToken(token)) {
            return Result.error(401, "Token无效或已过期");
        }

        AdminUser admin = adminAuthService.getAdminByToken(token);

        log.info("获取管理员信息: adminId={}, username={}", admin.get_id(), admin.getUsername());

        return Result.success("获取成功", admin);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "管理员退出登录（可选实现，客户端删除token即可）")
    public Result<String> logout(
            @Parameter(description = "Authorization header (Bearer token)")
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization != null) {
            String token = jwtTokenUtil.extractTokenFromHeader(authorization);
            if (token != null) {
                String adminId = jwtTokenUtil.getUserIdFromToken(token);
                log.info("管理员退出登录: adminId={}", adminId);
            }
        }

        return Result.success("退出成功");
    }
}

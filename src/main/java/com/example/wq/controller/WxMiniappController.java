package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.example.wq.entity.WqUser;
import com.example.wq.service.WxMiniappService;
import com.example.wq.util.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信小程序认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "微信小程序认证", description = "微信小程序登录和Token管理接口")
public class WxMiniappController {

    private final WxMiniappService wxMiniappService;
    private final JwtTokenUtil jwtTokenUtil;

    public WxMiniappController(WxMiniappService wxMiniappService,
                              JwtTokenUtil jwtTokenUtil) {
        this.wxMiniappService = wxMiniappService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 微信小程序一键登录
     */
    @PostMapping("/login")
    @Operation(summary = "微信小程序登录", description = "通过微信code实现一键登录，返回JWT token")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "微信小程序登录请求参数<br><b>参数说明：</b><br>" +
                "- <code>code</code>: 必填，微信小程序wx.login()获取的code<br>" +
                "- <code>nickname</code>: 可选，用户昵称<br>" +
                "- <code>avatar</code>: 可选，头像URL<br>" +
                "- <code>gender</code>: 可选，性别(0=未知,1=男,2=女)",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"code\":\"微信小程序wx.login获取的code\",\"nickname\":\"用户昵称\",\"avatar\":\"https://example.com/avatar.jpg\",\"gender\":1}"
            )
        )
    )
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> request) {

        String code = (String) request.get("code");
        if (code == null || code.trim().isEmpty()) {
            return Result.error("code不能为空");
        }

        String nickname = (String) request.get("nickname");
        String avatar = (String) request.get("avatar");
        Integer gender = request.get("gender") != null ?
                Integer.valueOf(request.get("gender").toString()) : null;

        log.info("微信小程序登录请求: code={}", code);

        Map<String, Object> response = wxMiniappService.login(code, nickname, avatar, gender);

        log.info("用户登录成功: userId={}, nickname={}", response.get("userId"), response.get("nickname"));

        return Result.success("登录成功", response);
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用refreshToken获取新的accessToken")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "刷新Token请求参数<br><b>参数说明：</b><br>" +
                "- <code>refreshToken</code>: 必填，登录时返回的refreshToken",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"refreshToken\":\"登录时返回的refreshToken\"}"
            )
        )
    )
    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Result.error(400, "refreshToken不能为空");
        }

        log.info("刷新Token请求");

        String newAccessToken = wxMiniappService.refreshAccessToken(refreshToken);

        return Result.success("Token刷新成功", Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer"
        ));
    }

    /**
     * 验证Token
     */
    @PostMapping("/validate")
    @Operation(summary = "验证Token", description = "验证JWT token是否有效")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "验证Token请求参数<br><b>参数说明：</b><br>" +
                "- <code>token</code>: 必填，完整的token字符串(包含或不包含Bearer前缀均可)",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"token\":\"Bearer eyJhbGciOiJIUzI1NiJ9...\"}"
            )
        )
    )
    public Result<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {

        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return Result.error(400, "token不能为空");
        }

        boolean isValid = wxMiniappService.validateToken(token);

        if (!isValid) {
            return Result.error(401, "Token无效或已过期");
        }

        String userId = jwtTokenUtil.getUserIdFromToken(token);
        String openid = jwtTokenUtil.getOpenidFromToken(token);

        return Result.success("Token有效", Map.of(
                "valid", true,
                "userId", userId,
                "openid", openid
        ));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info")
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户的信息")
    public Result<WqUser> getCurrentUserInfo(
            @Parameter(description = "Authorization header (Bearer token)", required = true)
            @RequestHeader("Authorization") String authorization) {

        String token = jwtTokenUtil.extractTokenFromHeader(authorization);
        if (token == null) {
            return Result.error(401, "无效的Authorization header格式");
        }

        if (!jwtTokenUtil.validateToken(token)) {
            return Result.error(401, "Token无效或已过期");
        }

        WqUser user = wxMiniappService.getUserByToken(token);

        log.info("获取用户信息: userId={}, nickname={}", user.get_id(), user.getNickname());

        return Result.success("获取成功", user);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录（可选实现，客户端删除token即可）")
    public Result<String> logout(
            @Parameter(description = "Authorization header (Bearer token)")
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization != null) {
            String token = jwtTokenUtil.extractTokenFromHeader(authorization);
            if (token != null) {
                log.info("用户退出登录: userId={}", jwtTokenUtil.getUserIdFromToken(token));
            }
        }

        return Result.success("退出成功");
    }
}

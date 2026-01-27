package com.example.wq.service;

import com.example.wq.config.JwtProperties;
import com.example.wq.config.WxMiniappProperties;
import com.example.wq.entity.WqUser;
import com.example.wq.enums.DeletedFlag;
import com.example.wq.repository.WqUserRepository;
import com.example.wq.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序登录服务
 */
@Slf4j
@Service
public class WxMiniappService {

    private final WxMiniappProperties wxMiniappProperties;
    private final JwtProperties jwtProperties;
    private final WqUserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WxMiniappService(WxMiniappProperties wxMiniappProperties,
                           JwtProperties jwtProperties,
                           WqUserRepository userRepository,
                           JwtTokenUtil jwtTokenUtil,
                           ObjectMapper objectMapper) {
        this.wxMiniappProperties = wxMiniappProperties;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    /**
     * 微信小程序一键登录
     *
     * @param code 微信登录code
     * @param nickname 用户昵称（可选）
     * @param avatar 用户头像（可选）
     * @param gender 性别（可选）
     * @return 登录响应（包含 token 和用户信息）
     */
    @Transactional
    public Map<String, Object> login(String code, String nickname, String avatar, Integer gender) {
        // 1. 调用微信接口获取 openid
        String openid = getWxOpenid(code);
        if (openid == null) {
            throw new RuntimeException("获取微信OpenID失败，请检查code是否有效");
        }

        // 2. 查找或创建用户
        WqUser user = findOrCreateUser(openid, nickname, avatar, gender);

        // 3. 生成 token
        String accessToken = jwtTokenUtil.generateAccessToken(user.get_id(), user.getOpenid());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.get_id(), user.getOpenid());

        // 4. 构建响应（包含完整用户信息和token信息）
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtProperties.getExpiration() / 1000);
        result.put("user", user);
        result.put("isNewUser", user.getCreateTime() != null &&
                           user.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(1)));

        return result;
    }
 
    /**
     * 刷新 Token
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        String openid = jwtTokenUtil.getOpenidFromToken(refreshToken);

        if (userId == null || openid == null) {
            throw new RuntimeException("无法从刷新令牌中获取用户信息");
        }

        return jwtTokenUtil.generateAccessToken(userId, openid);
    }

    /**
     * 调用微信接口获取 OpenID
     */
    private String getWxOpenid(String code) {
        try {
            String url = wxMiniappProperties.getCode2sessionUrl(code);
            log.info("调用微信接口获取OpenID，URL: {}", url);

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("微信接口响应: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("errcode")) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.get("errmsg").asText();
                log.error("微信接口返回错误: errcode={}, errmsg={}", errcode, errmsg);
                throw new RuntimeException("微信接口返回错误: " + errmsg);
            }

            String openid = jsonNode.get("openid").asText();
            log.info("成功获取OpenID: {}", openid);
            return openid;

        } catch (Exception e) {
            log.error("调用微信接口失败", e);
            throw new RuntimeException("调用微信接口失败: " + e.getMessage());
        }
    }

    /**
     * 查找或创建用户
     */
    private WqUser findOrCreateUser(String openid, String nickname, String avatar, Integer gender) {
        return userRepository.findByOpenidAndDeleted(openid, DeletedFlag.NOT_DELETED.getCode())
                .map(user -> {
                    updateUserIfPresent(user, nickname, avatar, gender);
                    return userRepository.save(user);
                })
                .orElseGet(() -> createNewUser(openid, nickname, avatar, gender));
    }

    /**
     * 更新用户信息
     */
    private void updateUserIfPresent(WqUser user, String nickname, String avatar, Integer gender) {
        boolean updated = false;

        if (nickname != null && !nickname.equals(user.getNickname())) {
            user.setNickname(nickname);
            updated = true;
        }

        if (avatar != null && !avatar.equals(user.getAvatar())) {
            user.setAvatar(avatar);
            updated = true;
        }

        if (gender != null && !gender.equals(user.getGender())) {
            user.setGender(gender);
            updated = true;
        }

        if (updated) {
            user.setUpdateTime(LocalDateTime.now());
        }
    }

    /**
     * 创建新用户
     */
    private WqUser createNewUser(String openid, String nickname, String avatar, Integer gender) {
        WqUser user = new WqUser();
        user.setOpenid(openid);
        user.setNickname(nickname != null ? nickname : "微信用户");
        user.setAvatar(avatar);
        user.setGender(gender != null ? gender : 0);
        user.setDeleted(DeletedFlag.NOT_DELETED.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        WqUser savedUser = userRepository.save(user);
        log.info("创建新用户: userId={}, openid={}", savedUser.get_id(), openid);
        return savedUser;
    }

    /**
     * 根据 Token 获取用户信息
     */
    public WqUser getUserByToken(String token) {
        String userId = jwtTokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无法从Token中获取用户ID");
        }

        return userRepository.findById(userId)
                .filter(user -> user.getDeleted().equals(DeletedFlag.NOT_DELETED.getCode()))
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        return jwtTokenUtil.validateToken(token);
    }
}

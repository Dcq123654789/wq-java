package com.example.wq.util;

import com.example.wq.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 */
@Slf4j
@Component
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;

    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成访问令牌
     *
     * @param userId 用户ID
     * @param openid 微信OpenID
     * @return JWT Token
     */
    public String generateAccessToken(String userId, String openid) {
        return generateToken(userId, openid, jwtProperties.getExpiration());
    }

    /**
     * 生成刷新令牌
     *
     * @param userId 用户ID
     * @param openid 微信OpenID
     * @return JWT Token
     */
    public String generateRefreshToken(String userId, String openid) {
        return generateToken(userId, openid, jwtProperties.getRefreshExpiration());
    }

    /**
     * 生成 Token
     *
     * @param userId 用户ID
     * @param openid 微信OpenID
     * @param expiration 过期时间（毫秒）
     * @return JWT Token
     */
    private String generateToken(String userId, String openid, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("openid", openid);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("userId", String.class);
        } catch (Exception e) {
            log.error("从Token中获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 从 Token 中获取 OpenID
     *
     * @param token JWT Token
     * @return OpenID
     */
    public String getOpenidFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("openid", String.class);
        } catch (Exception e) {
            log.error("从Token中获取OpenID失败", e);
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token为空: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Token验证失败", e);
        }
        return false;
    }

    /**
     * 检查 Token 是否即将过期（剩余时间小于1小时）
     *
     * @param token JWT Token
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long remainingTime = expiration.getTime() - now.getTime();
            return remainingTime < 3600000; // 1小时
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取 Token 过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 Token
     *
     * @param token JWT Token
     * @return Claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取签名密钥
     *
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从 Authorization Header 中提取 Token
     *
     * @param authHeader Authorization Header
     * @return Token
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(jwtProperties.getTokenPrefix() + " ")) {
            return authHeader.substring(jwtProperties.getTokenPrefix().length() + 1);
        }
        return null;
    }
}

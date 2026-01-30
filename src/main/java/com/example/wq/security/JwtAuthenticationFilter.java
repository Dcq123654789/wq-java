package com.example.wq.security;

import com.example.wq.config.JwtProperties;
import com.example.wq.entity.AdminUser;
import com.example.wq.entity.WqUser;
import com.example.wq.repository.AdminUserRepository;
import com.example.wq.repository.WqUserRepository;
import com.example.wq.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;

/**
 * JWT 认证过滤器
 *
 * 职责：
 * 1. 从请求头中提取 JWT Token
 * 2. 验证 Token 有效性
 * 3. 加载用户信息并设置到 SecurityContext（支持 WqUser 和 AdminUser）
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final WqUserRepository wqUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                   WqUserRepository wqUserRepository,
                                   AdminUserRepository adminUserRepository,
                                   JwtProperties jwtProperties) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.wqUserRepository = wqUserRepository;
        this.adminUserRepository = adminUserRepository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // 从请求中提取 token
        String token = extractTokenFromRequest(request);

        // 验证 token 并设置认证信息
        if (token != null && jwtTokenUtil.validateToken(token)) {
            String userId = jwtTokenUtil.getUserIdFromToken(token);
            String tokenType = jwtTokenUtil.getTokenTypeFromToken(token);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 根据 token 类型加载用户详情
                UserDetails userDetails = loadUserByToken(userId, tokenType);

                if (userDetails != null) {
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 认证成功: userId={}, tokenType={}", userId, tokenType);
                } else {
                    log.warn("用户不存在: userId={}, tokenType={}", userId, tokenType);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 根据 token 类型加载用户
     *
     * @param userId 用户ID
     * @param tokenType Token类型（user或admin）
     * @return UserDetails
     */
    private UserDetails loadUserByToken(String userId, String tokenType) {
        if (JwtTokenUtil.TOKEN_TYPE_USER.equals(tokenType)) {
            // 从 WqUser 表加载
            return wqUserRepository.findById(userId)
                    .map(user -> org.springframework.security.core.userdetails.User.builder()
                            .username(user.get_id())
                            .password("")
                            .authorities(Collections.emptyList())
                            .accountLocked(false)
                            .disabled(false)
                            .credentialsExpired(false)
                            .accountExpired(false)
                            .build())
                    .orElse(null);
        } else if (JwtTokenUtil.TOKEN_TYPE_ADMIN.equals(tokenType)) {
            // 从 AdminUser 表加载
            return adminUserRepository.findById(userId)
                    .map(admin -> org.springframework.security.core.userdetails.User.builder()
                            .username(admin.get_id())
                            .password("")
                            .authorities(Collections.emptyList())
                            .accountLocked(false)
                            .disabled(false)
                            .credentialsExpired(false)
                            .accountExpired(false)
                            .build())
                    .orElse(null);
        }
        return null;
    }

    /**
     * 从请求中提取 Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getTokenPrefix() + " ")) {
            return bearerToken.substring(jwtProperties.getTokenPrefix().length() + 1);
        }

        return null;
    }
}

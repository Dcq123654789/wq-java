package com.example.wq.security;

import com.example.wq.entity.WqUser;
import com.example.wq.repository.WqUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 自定义 UserDetailsService
 *
 * 职责：
 * 1. 根据 userId 加载用户信息
 * 2. 构建 UserDetails 对象供 Spring Security 使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final WqUserRepository wqUserRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // 这里 userId 实际上是用户的主键 ID
        return wqUserRepository.findById(userId)
                .map(this::createUserDetails)
                .orElseThrow(() -> {
                    log.warn("用户不存在: userId={}", userId);
                    return new UsernameNotFoundException("用户不存在: " + userId);
                });
    }

    /**
     * 创建 UserDetails 对象
     */
    private UserDetails createUserDetails(WqUser user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.get_id())
                .password("") // JWT 认证不需要密码
                .authorities(Collections.emptyList()) // 暂无角色/权限
                .accountLocked(false)
                .disabled(false)
                .credentialsExpired(false)
                .accountExpired(false)
                .build();
    }
}

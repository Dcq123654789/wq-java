package com.example.wq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置 - 完全开放模式
 *
 * 特性：
 * 1. 允许所有接口访问（无需认证）
 * 2. 配置 CORS 跨域支持
 * 3. 禁用 CSRF（API 接口不需要）
 * 4. 防止常见 Web 攻击（XSS、帧劫持等）
 * 5. 支持后续扩展为认证模式
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 配置 HTTP 安全
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（API 接口不需要）
            .csrf(AbstractHttpConfigurer::disable)

            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 配置权限：允许所有请求
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 允许所有接口访问
            )

            // 禁用默认登录页面
            .formLogin(AbstractHttpConfigurer::disable)

            // 禁用登出
            .logout(AbstractHttpConfigurer::disable)

            // 启用安全头（防止 XSS、点击劫持等）
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'"))
                .frameOptions(frame -> frame.sameOrigin())  // 防止点击劫持
                .httpStrictTransportSecurity(hsts -> hsts  // 强制 HTTPS
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .xssProtection(xss -> xss.headerValue(
                    org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            );

        return http.build();
    }

    /**
     * CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源（生产环境应该限制具体域名）
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));

        // 允许的请求方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // 允许携带凭证
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Content-Type", "Authorization", "X-Requested-With"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

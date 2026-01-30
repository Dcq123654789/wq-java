package com.example.wq.config;

import com.example.wq.security.CustomUserDetailsService;
import com.example.wq.security.JwtAuthenticationEntryPoint;
import com.example.wq.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置 - JWT 认证模式
 *
 * 特性：
 * 1. 放行用户端和管理端登录接口和接口文档
 * 2. 支持两种Token类型：user（用户端）和admin（管理端）
 * 3. 其他接口需要 JWT 认证
 * 4. 配置 CORS 跨域支持
 * 5. 禁用 CSRF（API 接口不需要）
 * 6. 防止常见 Web 攻击（XSS、帧劫持等）
 * 7. JWT 认证过滤器
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserDetailsService;

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

            // 配置异常处理（401 未授权）
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // 配置权限
            .authorizeHttpRequests(auth -> auth
                // 放行用户端登录相关接口（微信小程序）
                .requestMatchers("/api/auth/**").permitAll()

                // 放行管理端登录相关接口（后台管理系统）
                .requestMatchers("/api/admin/auth/**").permitAll()

                // 放行批量操作接口
                .requestMatchers("/api/batch/**").permitAll()

                // 放行文件上传接口
                .requestMatchers("/api/upload/**").permitAll()
                // 放行接口文档（Knife4j/Swagger）
                .requestMatchers(
                    "/doc.html",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/favicon.ico"
                ).permitAll()

                // 放行健康检查
                .requestMatchers("/actuator/**").permitAll()

                // 放行实体反射接口
                .requestMatchers("/api/entity/**").permitAll()

                // 放行通用CRUD接口
                .requestMatchers("/api/crud/**").permitAll()

                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )

            // 禁用默认登录页面
            .formLogin(AbstractHttpConfigurer::disable)

            // 禁用登出
            .logout(AbstractHttpConfigurer::disable)

            // 添加 JWT 认证过滤器（在 UsernamePasswordAuthenticationFilter 之前）
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 启用安全头（防止 XSS、点击劫持等）
            // 注意：为支持Knife4j文档界面，放宽了CSP策略
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self' data:;"))
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
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
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

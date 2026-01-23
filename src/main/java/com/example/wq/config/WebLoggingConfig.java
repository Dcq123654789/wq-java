package com.example.wq.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.time.Instant;

/**
 * Web请求日志拦截器
 * 自动记录所有HTTP请求和响应
 */
@Component
public class WebLoggingConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebLoggingConfig.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor());
    }

    private static class LoggingInterceptor implements HandlerInterceptor {

        private static final Logger log = LoggerFactory.getLogger("HTTP_REQUEST");

        private final ThreadLocal<Instant> startTime = new ThreadLocal<>();

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            startTime.set(Instant.now());

            String uri = request.getRequestURI();
            String method = request.getMethod();
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("→ 请求开始 | {} {} | IP: {} | UA: {}", method, uri, ip, userAgent);

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            Instant start = startTime.get();
            long duration = Duration.between(start, Instant.now()).toMillis();

            String uri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();

            if (ex != null) {
                log.error("← 请求异常 | {} {} | 状态: {} | 耗时: {}ms | 异常: {}",
                    method, uri, status, duration, ex.getMessage());
            } else if (status >= 400) {
                log.warn("← 请求失败 | {} {} | 状态: {} | 耗时: {}ms",
                    method, uri, status, duration);
            } else {
                log.info("← 请求完成 | {} {} | 状态: {} | 耗时: {}ms",
                    method, uri, status, duration);
            }

            startTime.remove();
        }

        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
    }
}

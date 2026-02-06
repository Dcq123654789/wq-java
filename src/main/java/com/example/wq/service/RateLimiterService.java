package com.example.wq.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务
 * 基于内存的限流实现（适用于单机）
 * 如需分布式限流，可改用 Redis + Lua
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    // 使用 Caffeine 缓存实现限流
    private final Cache<String, AtomicInteger> requestCounter;

    // 限流规则配置
    private static final int DEFAULT_LIMIT = 10;           // 默认限制次数
    private static final int DEFAULT_WINDOW_SECONDS = 60;   // 默认时间窗口（秒）

    public RateLimiterService() {
        this.requestCounter = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(DEFAULT_WINDOW_SECONDS))
                .maximumSize(10_000)
                .build();
    }

    /**
     * 检查是否允许请求（基于默认规则）
     *
     * @param key 限流键（如用户ID、IP地址）
     * @return true-允许请求，false-超过限制
     */
    public boolean allowRequest(String key) {
        return allowRequest(key, DEFAULT_LIMIT, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * 检查是否允许请求（自定义规则）
     *
     * @param key         限流键
     * @param limit       限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return true-允许请求，false-超过限制
     */
    public boolean allowRequest(String key, int limit, int windowSeconds) {
        AtomicInteger counter = requestCounter.get(key, k -> new AtomicInteger(0));

        int current = counter.incrementAndGet();

        if (current == 1) {
            // 第一次请求，设置过期时间（Caffeine 自动处理）
            log.debug("限流记录创建: key={}, limit={}, window={}s", key, limit, windowSeconds);
        }

        boolean allowed = current <= limit;

        if (!allowed) {
            log.warn("请求被限流: key={}, current={}, limit={}", key, current, limit);
        }

        return allowed;
    }

    /**
     * 重置限流计数（用于测试或管理员操作）
     *
     * @param key 限流键
     */
    public void reset(String key) {
        requestCounter.invalidate(key);
        log.info("限流计数已重置: key={}", key);
    }

    /**
     * 获取当前计数（用于监控）
     *
     * @param key 限流键
     * @return 当前请求次数
     */
    public int getCount(String key) {
        AtomicInteger counter = requestCounter.getIfPresent(key);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 预定义的限流规则
     */
    public static class Limits {
        /** 下单接口：每分钟最多 10 次 */
        public static final int ORDER_CREATE_LIMIT = 10;
        public static final int ORDER_CREATE_WINDOW = 60;

        /** 支付接口：每分钟最多 20 次 */
        public static final int ORDER_PAY_LIMIT = 20;
        public static final int ORDER_PAY_WINDOW = 60;

        /** 查询接口：每分钟最多 100 次 */
        public static final int QUERY_LIMIT = 100;
        public static final int QUERY_WINDOW = 60;
    }
}

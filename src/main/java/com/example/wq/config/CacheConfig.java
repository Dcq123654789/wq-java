package com.example.wq.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置 - 自动降级
 *
 * 如果 Redis 不可用，自动降级到 Caffeine 单级缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final long DEFAULT_EXPIRE_TIME = 10; // 默认过期时间（分钟）
    public static final int DEFAULT_MAX_SIZE = 1000;   // 默认最大缓存数

    /**
     * Caffeine 本地缓存管理器（主缓存管理器）
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // 用户缓存 - 10分钟过期，最多1000条
        cacheManager.setCaches(Arrays.asList(
            buildCache("userCache", 10, 1000),
            buildCache("productCache", 30, 500),
            buildCache("queryCache", 5, 2000),
            buildCache("demoCache", 10, 100)  // 演示缓存
        ));

        return cacheManager;
    }

    /**
     * 构建 Caffeine 缓存
     */
    private CaffeineCache buildCache(String cacheName, int expireMinutes, int maxSize) {
        return new CaffeineCache(cacheName,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .recordStats() // 记录统计信息
                .build()
        );
    }
}

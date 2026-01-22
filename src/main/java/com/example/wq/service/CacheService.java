package com.example.wq.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存服务 - 统一缓存操作接口（单级缓存版）
 *
 * 当前使用 Caffeine 本地缓存，无需 Redis
 */
@Service
public class CacheService {

    @Autowired
    private CacheManager cacheManager;

    /**
     * 获取缓存
     */
    public <T> T get(String cacheName, String key) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
        }
        return null;
    }

    /**
     * 设置缓存
     */
    public void set(String cacheName, String key, Object value) {
        set(cacheName, key, value, 10); // 默认10分钟
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String cacheName, String key, Object value, long minutes) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 删除缓存
     */
    public void evict(String cacheName, String key) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    /**
     * 清空缓存
     */
    public void clear(String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats(String cacheName) {
        Map<String, Object> stats = new HashMap<>();

        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats cacheStats = nativeCache.stats();

            stats.put("hitCount", cacheStats.hitCount());
            stats.put("missCount", cacheStats.missCount());
            stats.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
            stats.put("evictionCount", cacheStats.evictionCount());
            stats.put("size", nativeCache.estimatedSize());
            // maximumSize 无法从运行时获取，已省略
        }

        return stats;
    }

    /**
     * 获取缓存（别名，兼容旧代码）
     */
    public <T> T getLocal(String cacheName, String key) {
        return get(cacheName, key);
    }

    /**
     * 设置缓存（别名，兼容旧代码）
     */
    public void setLocal(String cacheName, String key, Object value) {
        set(cacheName, key, value);
    }

    /**
     * 删除缓存（别名，兼容旧代码）
     */
    public void evictLocal(String cacheName, String key) {
        evict(cacheName, key);
    }
}

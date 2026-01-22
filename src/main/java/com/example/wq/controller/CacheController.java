package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.example.wq.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存管理控制器
 *
 * 管理和监控 Caffeine 本地缓存
 */
@RestController
@RequestMapping("/admin/cache")
@Tag(name = "缓存管理", description = "缓存操作和监控接口")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats/{cacheName}")
    @Operation(summary = "获取缓存统计", description = "获取指定缓存的统计信息")
    public Result<Map<String, Object>> getStats(
        @Parameter(description = "缓存名称", example = "userCache")
        @PathVariable String cacheName) {

        Map<String, Object> stats = cacheService.getStats(cacheName);
        return Result.success(stats);
    }

    /**
     * 清空指定缓存
     */
    @DeleteMapping("/{cacheName}")
    @Operation(summary = "清空缓存", description = "清空指定缓存的所有数据")
    public Result<String> clear(
        @Parameter(description = "缓存名称", example = "userCache")
        @PathVariable String cacheName) {

        cacheService.clear(cacheName);
        return Result.success("缓存已清空");
    }

    /**
     * 删除缓存项
     */
    @DeleteMapping("/{cacheName}/{key}")
    @Operation(summary = "删除缓存项", description = "删除指定缓存中的某个键")
    public Result<String> evict(
        @Parameter(description = "缓存名称", example = "userCache")
        @PathVariable String cacheName,
        @Parameter(description = "缓存键", example = "user:123")
        @PathVariable String key) {

        cacheService.evict(cacheName, key);
        return Result.success("缓存项已删除");
    }

    /**
     * 手动设置缓存（示例）
     */
    @PostMapping("/{cacheName}")
    @Operation(summary = "设置缓存", description = "手动设置缓存项")
    public Result<String> set(
        @Parameter(description = "缓存名称", example = "userCache")
        @PathVariable String cacheName,
        @RequestBody Map<String, Object> payload) {

        String key = (String) payload.get("key");
        Object value = payload.get("value");
        Integer minutes = (Integer) payload.getOrDefault("minutes", 10);

        cacheService.set(cacheName, key, value, minutes);
        return Result.success("缓存已设置");
    }

    /**
     * 获取缓存项（示例）
     */
    @GetMapping("/{cacheName}/{key}")
    @Operation(summary = "获取缓存", description = "从缓存中获取数据")
    public Result<Object> get(
        @Parameter(description = "缓存名称", example = "userCache")
        @PathVariable String cacheName,
        @Parameter(description = "缓存键", example = "user:123")
        @PathVariable String key) {

        Object value = cacheService.get(cacheName, key);
        if (value != null) {
            return Result.success("缓存命中", value);
        } else {
            return Result.error("缓存未命中");
        }
    }

    /**
     * 预热缓存（示例）
     */
    @PostMapping("/warmup/{cacheName}")
    @Operation(summary = "预热缓存", description = "预先加载常用数据到缓存")
    public Result<String> warmup(
        @Parameter(description = "缓存名称", example = "userCache")
        @PathVariable String cacheName) {

        // 示例：预热一些常用数据
        for (int i = 1; i <= 10; i++) {
            String key = "user:" + i;
            String value = "User Data " + i;
            cacheService.set(cacheName, key, value, 30);
        }

        return Result.success("缓存预热完成，已加载10条数据");
    }

    /**
     * 获取所有缓存信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取所有缓存信息", description = "查看系统中配置的所有缓存")
    public Result<Map<String, Object>> getCacheInfo() {
        Map<String, Object> info = new java.util.HashMap<>();

        // 缓存列表
        info.put("caches", java.util.Arrays.asList(
            "userCache (用户缓存, 1000条, 10分钟)",
            "productCache (产品缓存, 500条, 30分钟)",
            "queryCache (查询缓存, 2000条, 5分钟)",
            "demoCache (演示缓存, 100条, 10分钟)"
        ));

        info.put("type", "Caffeine (本地缓存)");
        info.put("description", "单级缓存 - 无需 Redis，性能极佳");

        return Result.success(info);
    }
}

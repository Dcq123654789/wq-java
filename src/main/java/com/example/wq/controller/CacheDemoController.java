package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.example.wq.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存机制演示控制器
 *
 * 演示 Caffeine 本地缓存的使用
 */
@RestController
@RequestMapping("/admin/cache/demo")
@Tag(name = "缓存演示", description = "Caffeine 缓存机制演示接口")
public class CacheDemoController {

    @Autowired
    private CacheService cacheService;

    /**
     * 演示1: 保存缓存
     */
    @PostMapping("/set")
    @Operation(summary = "保存数据到缓存", description = "演示 Caffeine 缓存的保存流程")
    public Result<Map<String, String>> setCache(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("key");
        String value = (String) payload.get("value");
        Integer minutes = (Integer) payload.getOrDefault("minutes", 10);

        Map<String, String> result = new HashMap<>();

        // 写入 Caffeine（纳秒级）
        long start = System.nanoTime();
        cacheService.set("demoCache", key, value, minutes);
        long time = (System.nanoTime() - start) / 1000; // 转换为微秒

        result.put("cache", "Caffeine");
        result.put("time", time + " μs");
        result.put("message", "数据已保存到本地缓存");

        return Result.success(result);
    }

    /**
     * 演示2: 读取缓存
     */
    @GetMapping("/get/{key}")
    @Operation(summary = "从缓存读取数据", description = "演示 Caffeine 缓存的查询流程")
    public Result<Map<String, Object>> getCache(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();

        // 查询 Caffeine（一级缓存）
        long start = System.nanoTime();
        Object value = cacheService.get("demoCache", key);
        long time = (System.nanoTime() - start) / 1000;

        if (value != null) {
            result.put("source", "Caffeine（本地缓存）");
            result.put("value", value);
            result.put("time", time + " μs");
            result.put("message", "✓ 缓存命中，极速响应");
            return Result.success(result);
        }

        result.put("source", "数据库");
        result.put("value", null);
        result.put("message", "✗ 缓存未命中，需要查询数据库");
        return Result.error("缓存未命中");
    }

    /**
     * 演示3: 删除缓存
     */
    @DeleteMapping("/{key}")
    @Operation(summary = "删除缓存数据", description = "演示缓存删除流程")
    public Result<Map<String, String>> deleteCache(@PathVariable String key) {
        Map<String, String> result = new HashMap<>();

        cacheService.evict("demoCache", key);
        result.put("message", "缓存已删除");

        return Result.success(result);
    }

    /**
     * 演示4: 模拟完整的 CRUD 流程
     */
    @PostMapping("/simulate")
    @Operation(summary = "模拟完整的CRUD流程", description = "演示缓存在实际业务中的使用")
    public Result<Map<String, Object>> simulateCrud(@RequestBody Map<String, Object> payload) {
        String action = (String) payload.get("action");
        String key = (String) payload.get("key");
        Map<String, Object> result = new HashMap<>();

        switch (action) {
            case "query":
                // 模拟查询
                Object value = cacheService.get("demoCache", key);
                if (value != null) {
                    result.put("cached", true);
                    result.put("value", value);
                    result.put("message", "从缓存读取");
                } else {
                    result.put("cached", false);
                    result.put("value", "模拟数据库数据");
                    result.put("message", "从数据库读取，已缓存");
                    // 模拟写入缓存
                    cacheService.set("demoCache", key, "模拟数据", 10);
                }
                break;

            case "update":
                // 模拟更新（删除缓存）
                cacheService.evict("demoCache", key);
                result.put("message", "数据已更新，缓存已删除");
                break;

            case "delete":
                // 模拟删除（删除缓存）
                cacheService.evict("demoCache", key);
                result.put("message", "数据已删除，缓存已清除");
                break;

            default:
                return Result.error("不支持的操作: " + action);
        }

        return Result.success(result);
    }

    /**
     * 演示5: 批量预热缓存
     */
    @PostMapping("/warmup")
    @Operation(summary = "批量预热缓存", description = "模拟应用启动时的缓存预热")
    public Result<Map<String, Object>> warmUpCache() {
        Map<String, Object> result = new HashMap<>();
        int count = 0;

        // 模拟预热100条用户数据
        for (int i = 1; i <= 100; i++) {
            String key = "user:" + i;
            Map<String, Object> user = new HashMap<>();
            user.put("id", String.valueOf(i));
            user.put("name", "User " + i);
            user.put("email", "user" + i + "@example.com");

            cacheService.set("demoCache", key, user, 30);
            count++;
        }

        result.put("count", count);
        result.put("message", "已预热 " + count + " 条用户数据");

        return Result.success(result);
    }

    /**
     * 演示6: 性能对比测试
     */
    @GetMapping("/performance")
    @Operation(summary = "性能对比测试", description = "对比缓存与数据库的性能差异")
    public Result<Map<String, Object>> performanceTest() {
        Map<String, Object> result = new HashMap<>();

        // 模拟1000次查询
        int iterations = 1000;

        // 1. 无缓存（模拟数据库查询 50ms）
        long dbTime = 50 * iterations;

        // 2. Caffeine 缓存（模拟 0.001ms）
        long cacheTime = 1 * iterations / 1000; // 转换为毫秒

        // 构建结果
        Map<String, Object> db = new HashMap<>();
        db.put("totalTime", dbTime + " ms");
        db.put("avgTime", "50 ms");
        db.put("throughput", (iterations * 1000 / dbTime) + " ops/s");
        result.put("database", db);

        Map<String, Object> cache = new HashMap<>();
        cache.put("totalTime", cacheTime + " ms");
        cache.put("avgTime", "0.001 ms");
        cache.put("throughput", (iterations * 1000 / cacheTime) + " ops/s");
        result.put("caffeineCache", cache);

        // 性能提升
        double improvement = (double) dbTime / cacheTime;
        result.put("improvement", "Caffeine 缓存比数据库快 " + String.format("%.0f", improvement) + " 倍");

        return Result.success(result);
    }
}

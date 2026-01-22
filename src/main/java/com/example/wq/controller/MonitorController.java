package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 连接池监控接口
 *
 * 访问路径：GET /admin/pool-stats
 * 说明：查看 HikariCP 连接池状态
 */
@RestController
@RequestMapping("/admin")
@ConditionalOnClass(HikariDataSource.class)
public class MonitorController {

    @Autowired
    private DataSource dataSource;

    /**
     * 获取连接池状态
     */
    @GetMapping("/pool-stats")
    public Result<Map<String, Object>> getPoolStats() {
        if (!(dataSource instanceof HikariDataSource)) {
            return Result.error("数据源不是 HikariCP");
        }

        try {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolProxy = hikariDataSource.getHikariPoolMXBean();

            Map<String, Object> stats = new HashMap<>();
            stats.put("activeConnections", poolProxy.getActiveConnections());
            stats.put("idleConnections", poolProxy.getIdleConnections());
            stats.put("totalConnections", poolProxy.getTotalConnections());
            stats.put("threadsAwaitingConnection", poolProxy.getThreadsAwaitingConnection());

            // 从配置获取最大和最小连接数
            int maxConnections = hikariDataSource.getMaximumPoolSize();
            int minConnections = hikariDataSource.getMinimumIdle();
            stats.put("maxConnections", maxConnections);
            stats.put("minConnections", minConnections);

            // 计算使用率
            int activeConnections = poolProxy.getActiveConnections();
            double usagePercent = maxConnections > 0 ? (double) activeConnections / maxConnections * 100 : 0;
            stats.put("usagePercent", String.format("%.2f%%", usagePercent));

            // 额外信息
            stats.put("poolName", hikariDataSource.getPoolName());
            stats.put("jdbcUrl", hikariDataSource.getJdbcUrl());
            stats.put("username", hikariDataSource.getUsername());

            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取连接池状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // JVM 信息
        Runtime runtime = Runtime.getRuntime();
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("freeMemory", formatBytes(runtime.freeMemory()));
        info.put("totalMemory", formatBytes(runtime.totalMemory()));
        info.put("maxMemory", formatBytes(runtime.maxMemory()));
        info.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        // 推荐的连接池大小
        int processors = runtime.availableProcessors();
        int recommendedPoolSize = processors * 2 + 1;
        info.put("recommendedPoolSize", recommendedPoolSize);

        return Result.success(info);
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}

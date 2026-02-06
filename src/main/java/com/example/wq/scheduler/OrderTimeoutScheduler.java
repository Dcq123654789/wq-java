package com.example.wq.scheduler;

import com.example.wq.service.MallOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单超时处理定时任务
 * 每分钟检查一次超时未支付的订单，自动取消并释放库存
 */
@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    @Autowired
    private MallOrderService mallOrderService;

    /**
     * 处理超时订单
     * 每1分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒
    public void handleExpiredOrders() {
        try {
            Map<String, Object> result = mallOrderService.handleExpiredOrders();

            if (Boolean.TRUE.equals(result.get("success"))) {
                Integer cancelledCount = (Integer) result.get("cancelledCount");
                if (cancelledCount != null && cancelledCount > 0) {
                    log.info("定时任务处理超时订单: {}", result.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("定时任务处理超时订单失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期的库存预占记录
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 300秒 = 5分钟
    public void cleanExpiredLocks() {
        try {
            // 这里可以添加清理过期库存预占记录的逻辑
            // 实际的预占释放已在取消订单时完成
            log.debug("库存预占清理检查完成");
        } catch (Exception e) {
            log.error("清理过期库存预占失败: {}", e.getMessage(), e);
        }
    }
}

package com.example.wq.scheduler;

import com.example.wq.entity.CommunityActivity;
import com.example.wq.enums.ActivityStatus;
import com.example.wq.enums.DeletedFlag;
import com.example.wq.repository.CommunityActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动状态定时更新器
 * 负责定期检查并更新活动状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final CommunityActivityRepository activityRepository;

    /**
     * 定时检查报名截止时间（兜底方案）
     * 每天凌晨2点执行一次，处理长时间未被访问的活动
     *
     * 主要的状态更新逻辑在查询时实时完成，这里只是兜底处理：
     * - 处理那些长时间无人访问的活动
     * - 确保数据库中的状态最终一致性
     *
     * cron表达式: "0 0 2 * * ?" - 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkRegistrationDeadline() {
        try {
            log.info("定时任务开始检查活动报名截止时间（兜底任务）...");

            // 获取当前时间
            LocalDateTime currentDateTime = LocalDateTime.now();

            // 查找需要关闭报名的活动（用于日志记录）
            List<CommunityActivity> activitiesToClose = activityRepository.findActivitiesToCloseRegistration(
                    ActivityStatus.REGISTERING.getCode(),
                    currentDateTime,
                    DeletedFlag.NOT_DELETED.getCode()
            );

            if (activitiesToClose.isEmpty()) {
                log.info("没有需要关闭报名的活动");
                return;
            }

            // 批量更新活动状态
            int updatedCount = activityRepository.updateRegistrationClosedActivities(
                    ActivityStatus.REGISTERING.getCode(),      // 当前状态：报名中
                    ActivityStatus.REGISTRATION_CLOSED.getCode(), // 新状态：报名结束
                    currentDateTime,                            // 当前时间
                    DeletedFlag.NOT_DELETED.getCode()           // 未删除
            );

            log.info("定时任务成功更新 {} 个活动的状态为\"报名结束\"", updatedCount);

        } catch (Exception e) {
            log.error("定时任务检查活动报名截止时间时发生错误", e);
        }
    }

    /**
     * 手动触发检查报名截止时间（供测试或手动调用）
     * 可以通过其他服务调用此方法来立即执行状态检查
     */
    @Transactional(rollbackFor = Exception.class)
    public void manualCheckRegistrationDeadline() {
        log.info("手动触发检查活动报名截止时间...");
        checkRegistrationDeadline();
    }
}

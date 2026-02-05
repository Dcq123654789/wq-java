package com.example.wq.service;

import com.example.wq.entity.ActivityRegistration;
import com.example.wq.entity.CommunityActivity;
import com.example.wq.enums.DeletedFlag;
import com.example.wq.enums.PaymentStatus;
import com.example.wq.enums.RegistrationStatus;
import com.example.wq.repository.ActivityRegistrationRepository;
import com.example.wq.repository.CommunityActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * 社区活动服务
 */
@Slf4j
@Service
public class CommunityActivityService {

    private final CommunityActivityRepository activityRepository;
    private final ActivityRegistrationRepository registrationRepository;

    public CommunityActivityService(CommunityActivityRepository activityRepository,
                                    ActivityRegistrationRepository registrationRepository) {
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
    }

 
    /**
     * 检查用户是否已报名（简单版，仅返回布尔值）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 是否已报名
     */
    public boolean isUserRegistered(String activityId, String userId) {
        return registrationRepository.existsByActivityIdAndUserId(activityId, userId);
    }

    /**
     * 用户报名参加活动（优化版：事务控制 + 行锁 + 原子扣减）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param userName   用户姓名
     * @param userPhone  用户电话
     * @param remarks    备注
     * @return 报名结果（包含订单信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerActivity(String activityId, String userId,
                                                  String userName, String userPhone, String remarks) {
        log.info("用户开始报名: activityId={}, userId={}, userName={}", activityId, userId, userName);

        // 第一步：使用行锁查询活动（阻塞其他并发请求）
        CommunityActivity activity = activityRepository.findByIdWithLock(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        // 第二步：业务检查
        // 2.1 检查活动是否已删除
        if (!activity.getDeleted().equals(DeletedFlag.NOT_DELETED.getCode())) {
            throw new RuntimeException("活动不存在");
        }

        // 2.2 检查活动状态是否允许报名（0=报名中）
        if (activity.getStatus() != 0) {
            throw new RuntimeException("活动不在报名中");
        }

        // 2.3 检查报名是否截止
        if (activity.getRegistrationDeadlineTime() != null &&
                activity.getRegistrationDeadlineTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("报名已截止");
        }

        // 2.4 检查报名人数是否已满
        if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new RuntimeException("活动报名人数已满");
        }

        // 2.5 检查用户是否已经报名过（排除已取消的报名）
        if (registrationRepository.existsValidRegistrationByActivityIdAndUserId(activityId, userId)) {
            throw new RuntimeException("您已经报名过该活动");
        }

        // 第三步：原子扣减名额（使用 CAS 机制）
        int updatedRows = activityRepository.incrementParticipantsAtomically(
                activityId,
                activity.getCurrentParticipants(),
                activity.getMaxParticipants()
        );

        if (updatedRows == 0) {
            // 名额在检查和更新之间被抢光了
            throw new RuntimeException("活动名额已满，请稍后再试");
        }

        // 第四步：创建报名记录
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(userId);
        registration.setUserName(userName);
        registration.setUserPhone(userPhone);
        registration.setRemarks(remarks);
        registration.setStatus(RegistrationStatus.REGISTERED.getCode());

        // 判断是免费还是付费活动
        boolean isFreeActivity = activity.getPrice() == null || activity.getPrice().compareTo(BigDecimal.ZERO) <= 0;
        String orderNo = generateOrderNo();

        if (isFreeActivity) {
            // 免费活动：直接标记为已支付
            registration.setPaymentStatus(PaymentStatus.PAID.getCode());
            registration.setPaymentAmount(BigDecimal.ZERO);
            registration.setPaymentTime(LocalDateTime.now());
            log.info("免费活动报名成功，直接标记为已支付");
        } else {
            // 付费活动：状态为未支付，设置过期时间
            registration.setPaymentStatus(PaymentStatus.UNPAID.getCode());
            registration.setPaymentAmount(activity.getPrice());
            // 支付过期时间：15分钟
            registration.setPaymentExpireTime(LocalDateTime.now().plusMinutes(15));
            log.info("付费活动报名成功，待支付，金额: {}", activity.getPrice());
        }

        // 存储订单号（需要在实体中添加此字段）
        registration.setOrderNo(orderNo);

        ActivityRegistration savedRegistration = registrationRepository.save(registration);

        // 第五步：构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("registrationId", savedRegistration.get_id());
        result.put("orderNo", orderNo);
        result.put("isFree", isFreeActivity);
        result.put("needPayment", !isFreeActivity);
        result.put("paymentAmount", activity.getPrice());

        log.info("用户报名成功: registrationId={}, activityId={}, userId={}, isFree={}",
                savedRegistration.get_id(), activityId, userId, isFreeActivity);

        return result;
    }

    /**
     * 生成唯一订单号
     */
    private String generateOrderNo() {
        return "ACT" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 取消报名
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param cancelReason 取消原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(String activityId, String userId, String cancelReason) {
        log.info("用户取消报名: activityId={}, userId={}", activityId, userId);

        // 1. 查找报名记录
        ActivityRegistration registration = registrationRepository.findByActivityIdAndUserId(activityId, userId)
                .orElseThrow(() -> new RuntimeException("未找到报名记录"));

        // 2. 检查报名状态（只有已报名状态才能取消）
        if (registration.getStatus() != 0) {
            throw new RuntimeException("当前状态不能取消报名");
        }

        // 3. 更新报名状态
        registration.setStatus(1); // 1=已取消
        registration.setCancelTime(java.time.LocalDateTime.now());
        registration.setCancelReason(cancelReason);
        registrationRepository.save(registration);

        // 4. 减少活动参与人数
        CommunityActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        if (activity.getCurrentParticipants() > 0) {
            activity.setCurrentParticipants(activity.getCurrentParticipants() - 1);
            activityRepository.save(activity);
        }

        log.info("取消报名成功: activityId={}, userId={}", activityId, userId);
    }

}

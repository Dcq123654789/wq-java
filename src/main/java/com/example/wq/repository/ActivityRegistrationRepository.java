package com.example.wq.repository;

import com.example.wq.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 活动报名 Repository
 */
@Repository
public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, String> {

 
    /**
     * 检查用户是否已报名某个活动
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 是否已报名
     */
    boolean existsByActivityIdAndUserId(String activityId, String userId);

    /**
     * 根据活动ID和用户ID查找报名记录
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 报名记录
     */
    Optional<ActivityRegistration> findByActivityIdAndUserId(String activityId, String userId);

    /**
     * 检查用户是否有已报名或待支付的报名记录（排除已取消的）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 是否存在有效报名记录
     */
    @Query("SELECT COUNT(r) > 0 FROM ActivityRegistration r " +
            "WHERE r.activityId = :activityId " +
            "AND r.userId = :userId " +
            "AND r.status != 1")
    boolean existsValidRegistrationByActivityIdAndUserId(
            @Param("activityId") String activityId,
            @Param("userId") String userId
    );

    /**
     * 检查用户是否有待支付的报名记录
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 是否存在待支付报名记录
     */
    @Query("SELECT COUNT(r) > 0 FROM ActivityRegistration r " +
            "WHERE r.activityId = :activityId " +
            "AND r.userId = :userId " +
            "AND r.paymentStatus = 0")
    boolean existsPendingPaymentByActivityIdAndUserId(
            @Param("activityId") String activityId,
            @Param("userId") String userId
    );

    /**
     * 根据订单号查询报名记录
     *
     * @param orderNo 订单号
     * @return 报名记录
     */
    Optional<ActivityRegistration> findByOrderNo(String orderNo);

    /**
     * 根据报名记录ID和订单号查询
     *
     * @param _id     报名记录ID
     * @param orderNo 订单号
     * @return 报名记录
     */
    @Query("SELECT r FROM ActivityRegistration r WHERE r._id = :_id AND r.orderNo = :orderNo")
    Optional<ActivityRegistration> findByIdAndOrderNo(
            @Param("_id") String _id,
            @Param("orderNo") String orderNo
    );

    }

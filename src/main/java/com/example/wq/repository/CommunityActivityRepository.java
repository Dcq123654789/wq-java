package com.example.wq.repository;

import com.example.wq.entity.CommunityActivity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 社区活动 Repository
 */
@Repository
public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, String> {

    /**
     * 查找所有报名中的活动
     *
     * @return 报名中的活动列表
     */
    List<CommunityActivity> findByStatus(Integer status);

    /**
     * 批量更新报名截止时间已到的活动状态为"报名结束"
     *
     * @param currentStatus 当前状态（报名中）
     * @param newStatus     新状态（报名结束）
     * @param currentDateTime 当前时间
     * @param deleted       未删除标记
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE CommunityActivity a " +
            "SET a.status = :newStatus " +
            "WHERE a.status = :currentStatus " +
            "AND a.registrationDeadlineTime IS NOT NULL " +
            "AND a.registrationDeadlineTime <= :currentDateTime " +
            "AND a.deleted = :deleted")
    int updateRegistrationClosedActivities(
            @Param("currentStatus") Integer currentStatus,
            @Param("newStatus") Integer newStatus,
            @Param("currentDateTime") LocalDateTime currentDateTime,
            @Param("deleted") Integer deleted
    );

    /**
     * 查找需要关闭报名的活动列表
     *
     * @param currentStatus 当前状态
     * @param currentDateTime 当前时间
     * @param deleted       未删除标记
     * @return 需要更新的活动列表
     */
    @Query("SELECT a FROM CommunityActivity a " +
            "WHERE a.status = :currentStatus " +
            "AND a.registrationDeadlineTime IS NOT NULL " +
            "AND a.registrationDeadlineTime <= :currentDateTime " +
            "AND a.deleted = :deleted")
    List<CommunityActivity> findActivitiesToCloseRegistration(
            @Param("currentStatus") Integer currentStatus,
            @Param("currentDateTime") LocalDateTime currentDateTime,
            @Param("deleted") Integer deleted
    );

    /**
     * 使用行锁查询活动（用于高并发报名场景）
     *
     * @param activityId 活动ID
     * @return 活动信息（带行锁）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CommunityActivity a WHERE a._id = :activityId")
    Optional<CommunityActivity> findByIdWithLock(@Param("activityId") String activityId);

    /**
     * 原子扣减名额（条件更新）
     * 只有当当前人数小于最大人数时才更新成功
     *
     * @param activityId 活动ID
     * @param currentParticipants 当前参与人数（用于 CAS 检查）
     * @param maxParticipants 最大参与人数
     * @return 更新的记录数（1 表示成功，0 表示名额已满）
     */
    @Modifying
    @Query("UPDATE CommunityActivity a " +
            "SET a.currentParticipants = a.currentParticipants + 1 " +
            "WHERE a._id = :activityId " +
            "AND a.currentParticipants = :currentParticipants " +
            "AND a.currentParticipants < :maxParticipants")
    int incrementParticipantsAtomically(
            @Param("activityId") String activityId,
            @Param("currentParticipants") Integer currentParticipants,
            @Param("maxParticipants") Integer maxParticipants
    );

    /**
     * 根据活动ID和未删除状态查找活动
     *
     * @param activityId 活动ID
     * @param deleted    未删除标记
     * @return 活动信息
     */
    @Query("SELECT a FROM CommunityActivity a WHERE a._id = :activityId AND a.deleted = :deleted")
    Optional<CommunityActivity> findByIdAndDeleted(
            @Param("activityId") String activityId,
            @Param("deleted") Integer deleted
    );
}

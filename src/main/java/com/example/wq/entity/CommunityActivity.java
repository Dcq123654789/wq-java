package com.example.wq.entity;

import com.example.wq.annotation.ExcludeField;
import com.example.wq.enums.ActivityCategory;
import com.example.wq.enums.ActivityStatus;
import com.example.wq.enums.AuditStatus;
import com.example.wq.enums.DeletedFlag;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区活动实体
 */
@Entity
@Table(name = "community_activity", indexes = {
    @Index(name = "idx_audit_status", columnList = "audit_status"),
    @Index(name = "idx_status_start_time", columnList = "status,activity_start_time")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "社区活动实体")
public class CommunityActivity extends AbstractHibernateBean {

    @Schema(description = "活动标题", example = "社区书法交流活动")
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Schema(description = "封面图片URL", example = "https://example.com/image.jpg")
    @Column(name = "cover_image", length = 500)
    private List<String>  coverImage;

    @Schema(description = "活动描述", example = "邀请社区书法爱好者共同交流学习...")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Schema(description = "活动开始时间", example = "2024-01-20T14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "activity_start_time", nullable = false)
    private LocalDateTime activityStartTime;

    @Schema(description = "活动结束时间", example = "2024-01-20T16:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "activity_end_time", nullable = false)
    private LocalDateTime activityEndTime;

    @Schema(description = "活动地点名称", example = "社区文化活动中心")
    @Column(name = "location_name", length = 200, nullable = false)
    private String locationName;

    @Schema(description = "详细地址", example = "晚晴社区服务中心 2 楼书法室")
    @Column(name = "location_address", length = 500, nullable = false)
    private String locationAddress;

    @Schema(description = "纬度", example = "39.9042")
    @Column(name = "latitude")
    private Double latitude;

    @Schema(description = "经度", example = "116.4074")
    @Column(name = "longitude")
    private Double longitude;

    @Schema(description = "最大参与人数", example = "20")
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Schema(description = "当前参与人数", example = "15")
    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants = 0;
 

    @Schema(description = "活动负责人", example = "王老师")
    @Column(name = "organizer_name", length = 100, nullable = false)
    private String organizerName;
 

    @Schema(description = "组织者联系电话", example = "138****8888")
    @Column(name = "organizer_phone", length = 20)
    private String organizerPhone;

    @Schema(description = "报名截止时间", example = "2024-01-19T18:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "registration_deadline_time")
    private LocalDateTime registrationDeadlineTime;

    @Schema(description = "活动图片数组（JSON数组）", example = "[\"https://example.com/img1.jpg\", \"https://example.com/img2.jpg\"]")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "json")
    private List<String> images;

    @Schema(description = "活动标签数组（JSON数组）", example = "[\"书法\", \"文化交流\", \"免费\"]")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags;  

    @Schema(description = "报名状态", example = "0", allowableValues = {"0", "1", "2", "3"})
    @Column(name = "status", nullable = false)
    private Integer status = ActivityStatus.REGISTERING.getCode();

    @Schema(description = "活动分类", example = "0", allowableValues = {"0", "1", "2", "3", "4"})
    @Column(name = "category", nullable = false)
    private Integer category;

    @Schema(description = "活动费用（0表示免费）", example = "0.00")
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;


    @Schema(description = "社区ID（多社区场景）", example = "COMM001")
    @Column(name = "community_id", length = 64)
    private String communityId;

    @ExcludeField
    @Schema(description = "逻辑删除", example = "0", allowableValues = {"0", "1"})
    @Column(name = "deleted", nullable = false)
    private Integer deleted = DeletedFlag.NOT_DELETED.getCode();

    // ========== 审核相关字段 ==========

    @Schema(description = "审核状态", example = "0", allowableValues = {"0", "1", "2"})
    @Column(name = "audit_status", nullable = false)
    private Integer auditStatus = AuditStatus.PENDING_AUDIT.getCode();

    @Schema(description = "审核人ID", example = "admin123")
    @Column(name = "auditor_id", length = 64)
    private String auditorId;

    @Schema(description = "审核时间", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注/拒绝原因", example = "活动信息不完整，请补充活动详情")
    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    // ========== 关联关系 ==========
    @ExcludeField
    @Schema(description = "所属社区信息（懒加载）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", insertable = false, updatable = false)
    private Community community;

    // ========== 枚举转换方法 ==========

    /**
     * 获取活动状态枚举
     */
    @Transient
    public ActivityStatus getStatusEnum() {
        return ActivityStatus.fromCode(this.status);
    }

    /**
     * 设置活动状态
     */
    public void setStatusEnum(ActivityStatus activityStatus) {
        this.status = activityStatus != null ? activityStatus.getCode() : null;
    }

    /**
     * 获取活动分类枚举
     */
    @Transient
    public ActivityCategory getCategoryEnum() {
        return ActivityCategory.fromCode(this.category);
    }

    /**
     * 设置活动分类
     */
    public void setCategoryEnum(ActivityCategory activityCategory) {
        this.category = activityCategory != null ? activityCategory.getCode() : null;
    }

    /**
     * 获取审核状态枚举
     */
    @Transient
    public AuditStatus getAuditStatusEnum() {
        return AuditStatus.fromCode(this.auditStatus);
    }

    /**
     * 设置审核状态
     */
    public void setAuditStatusEnum(AuditStatus auditStatus) {
        this.auditStatus = auditStatus != null ? auditStatus.getCode() : null;
    }

    /**
     * 获取删除标记枚举
     */
    @Transient
    public DeletedFlag getDeletedEnum() {
        return DeletedFlag.fromCode(this.deleted);
    }

    /**
     * 设置删除标记
     */
    public void setDeletedEnum(DeletedFlag deletedFlag) {
        this.deleted = deletedFlag != null ? deletedFlag.getCode() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
        if (this.status == null) {
            this.status = ActivityStatus.REGISTERING.getCode();
        }
        if (this.auditStatus == null) {
            this.auditStatus = AuditStatus.PENDING_AUDIT.getCode();
        }
        if (this.currentParticipants == null) {
            this.currentParticipants = 0;
        }
        if (this.deleted == null) {
            this.deleted = DeletedFlag.NOT_DELETED.getCode();
        }
        if (this.price == null) {
            this.price = BigDecimal.ZERO;
        }
    }
}

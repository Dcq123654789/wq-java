package com.example.wq.entity;

import com.example.wq.annotation.ExcludeField;
import com.example.wq.enums.PaymentStatus;
import com.example.wq.enums.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动报名实体
 */
@Entity
@Table(name = "activity_registration", indexes = {
    @Index(name = "uk_activity_user", columnList = "activity_id,user_id", unique = true),
 })
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "活动报名实体")
public class ActivityRegistration extends AbstractHibernateBean {

    @Schema(description = "活动ID", example = "1234567890")
    @Column(name = "activity_id", length = 64, nullable = false)
    private String activityId;

    @Schema(description = "用户ID", example = "user123")
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Schema(description = "用户姓名", example = "张阿姨")
    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    @Schema(description = "联系电话", example = "139****1234")
    @Column(name = "user_phone", length = 20)
    private String userPhone;

    @Schema(description = "报名时间", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "registration_time", nullable = false)
    @CreationTimestamp
    private LocalDateTime registrationTime;

    @Schema(description = "报名状态", example = "0", allowableValues = {"0", "1", "2"})
    @Column(name = "status", nullable = false)
    private Integer status = RegistrationStatus.REGISTERED.getCode();

    @Schema(description = "支付状态", example = "0", allowableValues = {"0", "1", "2"})
    @Column(name = "payment_status")
    private Integer paymentStatus = PaymentStatus.UNPAID.getCode();

    @Schema(description = "支付金额", example = "50.00")
    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Schema(description = "支付时间", example = "2024-01-15 10:31:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Schema(description = "备注信息", example = "期待参加活动")
    @Column(name = "remarks", length = 500)
    private String remarks;

    @Schema(description = "取消时间", example = "2024-01-16 09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因", example = "临时有事无法参加")
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Schema(description = "订单号", example = "ACT1705321234567890ABC123")
    @Column(name = "order_no", length = 64, unique = true)
    private String orderNo;

    @Schema(description = "支付过期时间", example = "2024-01-15 10:46:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "payment_expire_time")
    private LocalDateTime paymentExpireTime;

    // ========== 关联关系 ==========

    @Schema(description = "关联的活动（懒加载）")
    @ManyToOne(fetch = FetchType.LAZY)
    @ExcludeField
    @JoinColumn(name = "activity_id", insertable = false, updatable = false)
    private CommunityActivity activity;

    @Schema(description = "关联的用户（懒加载）")
    @ManyToOne(fetch = FetchType.LAZY)
    @ExcludeField
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private WqUser user;

    // ========== 枚举转换方法 ==========

    /**
     * 获取报名状态枚举
     */
    @Transient
    public RegistrationStatus getStatusEnum() {
        return RegistrationStatus.fromCode(this.status);
    }

    /**
     * 设置报名状态
     */
    public void setStatusEnum(RegistrationStatus registrationStatus) {
        this.status = registrationStatus != null ? registrationStatus.getCode() : null;
    }

    /**
     * 获取支付状态枚举
     */
    @Transient
    public PaymentStatus getPaymentStatusEnum() {
        return PaymentStatus.fromCode(this.paymentStatus);
    }

    /**
     * 设置支付状态
     */
    public void setPaymentStatusEnum(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus != null ? paymentStatus.getCode() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
        if (this.status == null) {
            this.status = RegistrationStatus.REGISTERED.getCode();
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.UNPAID.getCode();
        }
        if (this.paymentAmount == null) {
            this.paymentAmount = BigDecimal.ZERO;
        }
    }
}

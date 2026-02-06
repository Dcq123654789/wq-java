package com.example.wq.entity;

import com.example.wq.annotation.ExcludeField;
import com.example.wq.enums.DeletedFlag;
import com.example.wq.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

// import java.math.BigDecimal;

/**
 * WQ用户实体
 */
@Entity
@Table(name = "wq_user")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "WQ用户实体")
public class WqUser extends AbstractHibernateBean {

    @Schema(description = "微信OpenID", example = "oX1234567890abcdef")
    @Column(name = "openid", length = 64, unique = true)
    private String openid;

    @Schema(description = "微信UnionID", example = "ux1234567890abcdef")
    @Column(name = "unionid", length = 64)
    private String unionid; 
 
        
    @Schema(description = "昵称", example = "张三")
    @Column(name = "nickname", length = 50)
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @Column(name = "avatar", length = 255)
    private String avatar;

    @Schema(description = "手机号", example = "13800138000")
    @Column(name = "phone", length = 20)
    private String phone;

    @Schema(description = "真实姓名", example = "张三")
    @Column(name = "real_name", length = 50)
    private String realName;

    @Schema(description = "性别", example = "1", allowableValues = {"0", "1", "2"})
    @Column(name = "gender")
    private Integer gender = Gender.UNKNOWN.getCode();

    @Schema(description = "出生日期", example = "1990-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Schema(description = "省份", example = "广东省")
    @Column(name = "province", length = 50)
    private String province;

    @Schema(description = "城市", example = "深圳市")
    @Column(name = "city", length = 50)
    private String city;

    @Schema(description = "区/县", example = "南山区")
    @Column(name = "district", length = 50)
    private String district;

    @Schema(description = "详细地址", example = "科技园南区XX大厦")
    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Schema(description = "所属社区ID", example = "COMM001")
    @Column(name = "community_id", length = 64)
    private String communityId;

    @Schema(description = "所属社区名称", example = "COMM001")
    @Column(name = "communityName", length = 64)
    private String communityName;

    @Schema(description = "所属社区信息（懒加载）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", insertable = false, updatable = false)
    private Community community;

    @Schema(description = "用户收货地址列表（懒加载）")
    @OneToMany(mappedBy = "wquser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserAddress> addresses = new ArrayList<>();

    //    @Schema(description = "账户余额（分）", example = "10000")
    //    @Column(name = "balance", precision = 19, scale = 2)
    //    private Long balance = 0L;
    
    @ExcludeField
    @Schema(description = "逻辑删除", example = "0", allowableValues = {"0", "1"})
    @Column(name = "deleted")
    private Integer deleted = DeletedFlag.NOT_DELETED.getCode();

    /**
     * 获取性别枚举
     */
    @Transient
    public Gender getGenderEnum() {
        return Gender.fromCode(this.gender);
    }

    /**
     * 设置性别
     */
    public void setGenderEnum(Gender gender) {
        this.gender = gender != null ? gender.getCode() : Gender.UNKNOWN.getCode();
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

    //    /**
    //     * 获取账户余额（元）
    //     */
    //    @Transient
    //    public BigDecimal getBalanceInYuan() {
    //        return this.balance != null ? new BigDecimal(this.balance).divide(new BigDecimal("100")) : BigDecimal.ZERO;
    //    }
    //
    //    /**
    //     * 设置账户余额（元）
    //     */
    //    public void setBalanceInYuan(BigDecimal balanceInYuan) {
    //        this.balance = balanceInYuan != null ? balanceInYuan.multiply(new BigDecimal("100")).longValue() : 0L;
    //    }
    //
    //    /**
    //     * 增加余额
    //     */
    //    public void addBalance(Long amount) {
    //        if (amount != null && amount > 0) {
    //            this.balance = (this.balance != null ? this.balance : 0L) + amount;
    //        }
    //    }
    //
    //    /**
    //     * 扣减余额
    //     */
    //    public void deductBalance(Long amount) {
    //        if (amount != null && amount > 0) {
    //            this.balance = (this.balance != null ? this.balance : 0L) - amount;
    //        }
    //    }

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
        //        if (this.status == null) {
        //            this.status = UserStatus.NORMAL.getCode();
        //        }
        if (this.deleted == null) {
            this.deleted = DeletedFlag.NOT_DELETED.getCode();
        }
        if (this.gender == null) {
            this.gender = Gender.UNKNOWN.getCode();
        }
        //        if (this.balance == null) {
        //            this.balance = 0L;
        //        }
    }
}

package com.example.wq.entity;

import com.example.wq.enums.AddressTag;
import com.example.wq.enums.YesNo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户收货地址实体
 */
@Entity
@Table(name = "user_address", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_is_default", columnList = "is_default")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户收货地址实体")
public class UserAddress extends AbstractHibernateBean {

    @Schema(description = "关联用户（懒加载）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private WqUser wquser;

    @Schema(description = "用户ID", example = "1703123456789_1234")
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Schema(description = "收货人姓名", example = "张三")
    @Column(name = "receiver_name", length = 50, nullable = false)
    private String receiverName;

    @Schema(description = "收货人电话", example = "13800138000")
    @Column(name = "receiver_phone", length = 20, nullable = false)
    private String receiverPhone;

    @Schema(description = "省份", example = "广东省")
    @Column(name = "province", length = 50, nullable = false)
    private String province;

    @Schema(description = "城市", example = "深圳市")
    @Column(name = "city", length = 50, nullable = false)
    private String city;

    @Schema(description = "区/县", example = "南山区")
    @Column(name = "district", length = 50)
    private String district;

    @Schema(description = "详细地址", example = "科技园南区XX大厦")
    @Column(name = "detail_address", length = 255, nullable = false)
    private String detailAddress;

    @Schema(description = "邮政编码", example = "518000")
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Schema(description = "是否默认地址", example = "0", allowableValues = {"0", "1"})
    @Column(name = "is_default", nullable = false)
    private Integer isDefault = YesNo.NO.getCode();

    @Schema(description = "地址标签", example = "0", allowableValues = {"0", "1", "2"})
    @Column(name = "tag", length = 20)
    private Integer tag;

    @Schema(description = "使用次数", example = "5")
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
        if (this.usedCount == null) {
            this.usedCount = 0;
        }
    }

    /**
     * 获取完整地址
     */
    @Transient
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (province != null) sb.append(province);
        if (city != null) sb.append(city);
        if (district != null) sb.append(district);
        if (detailAddress != null) sb.append(detailAddress);
        return sb.toString();
    }
}

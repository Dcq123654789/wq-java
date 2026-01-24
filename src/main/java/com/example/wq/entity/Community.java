package com.example.wq.entity;

import com.example.wq.enums.DeletedFlag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社区实体
 */
@Entity
@Table(name = "community")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "社区实体")
public class Community extends AbstractHibernateBean {

    @Schema(description = "社区编码", example = "COMM001")
    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Schema(description = "社区名称", example = "阳光社区")
    @Column(name = "name", length = 100)
    private String name;

    @Schema(description = "省份", example = "广东省")
    @Column(name = "province", length = 50)
    private String province;

    @Schema(description = "城市", example = "深圳市")
    @Column(name = "city", length = 50)
    private String city;

    @Schema(description = "区/县", example = "南山区")
    @Column(name = "district", length = 50)
    private String district;

    @Schema(description = "详细地址", example = "科技园南区XX路XX号")
    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Schema(description = "纬度", example = "22.5431")
    @Column(name = "latitude")
    private Double latitude;

    @Schema(description = "经度", example = "114.0579")
    @Column(name = "longitude")
    private Double longitude;

    @Schema(description = "联系人", example = "张三")
    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Schema(description = "社区描述", example = "这是一个现代化的住宅社区")
    @Column(name = "description", length = 500)
    private String description;

    @Schema(description = "逻辑删除", example = "0", allowableValues = {"0", "1"})
    @Column(name = "deleted")
    private Integer deleted = DeletedFlag.NOT_DELETED.getCode();

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
        if (this.deleted == null) {
            this.deleted = DeletedFlag.NOT_DELETED.getCode();
        }
    }
}

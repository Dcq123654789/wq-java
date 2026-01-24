package com.example.wq.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Hibernate实体基础类
 */
@MappedSuperclass
@Data
@Schema(description = "实体基础类")
public abstract class AbstractHibernateBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实体ID", example = "1703123456789_1234")
    @Id
    @Column(name = "_id")
    private String _id;

    @Schema(description = "创建时间", example = "2024-01-22 14:30:00")
    @Column(name = "create_time")
    @CreationTimestamp
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-22 14:30:00")
    @Column(name = "update_time")
    @UpdateTimestamp
    private LocalDateTime updateTime;

    @Schema(description = "状态：0-禁用 1-正常", example = "1")
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 生成唯一ID
     */
    public static String generateId() {
        return String.valueOf(System.currentTimeMillis()) + "_" + String.valueOf((int)(Math.random() * 10000));
    }
}


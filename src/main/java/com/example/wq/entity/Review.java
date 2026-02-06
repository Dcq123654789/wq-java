package com.example.wq.entity;

import com.example.wq.annotation.ExcludeField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品评价实体
 */
@Entity
@Table(name = "review", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品评价实体")
public class Review extends AbstractHibernateBean {

    @Schema(description = "商品ID", example = "1703123456789_5678")
    @Column(name = "product_id", length = 64, nullable = false)
    private String productId;

    @Schema(description = "订单ID", example = "1703123456789_1234")
    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Schema(description = "用户ID", example = "1703123456789_9999")
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Schema(description = "评分（1-5星）", example = "5")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Schema(description = "评价内容", example = "商品质量很好，物流也快")
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // ========== 关联关系 ==========

    @ExcludeField
    @Schema(description = "关联商品")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ExcludeField
    @Schema(description = "关联订单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
    }
}

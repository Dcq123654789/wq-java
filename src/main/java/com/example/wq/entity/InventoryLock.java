package com.example.wq.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存预占实体
 * 用于下单时锁定库存，防止超卖
 */
@Entity
@Table(name = "inventory_lock", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_expire_time", columnList = "expire_time"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "库存预占实体")
public class InventoryLock extends AbstractHibernateBean {

    @Schema(description = "商品ID", example = "1703123456789_123")
    @Column(name = "product_id", length = 64, nullable = false)
    private String productId;

    @Schema(description = "订单ID", example = "1703123456789_456")
    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Schema(description = "预占数量", example = "2")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Schema(description = "过期时间（秒级时间戳）", example = "1704067200")
    @Column(name = "expire_time", nullable = false)
    private Long expireTime;

    @Schema(description = "状态：0-已释放，1-已锁定", example = "1")
    @Column(name = "status", nullable = false)
    private Integer status;

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
        if (this.status == null) {
            this.status = 1; // 默认已锁定
        }
    }
}

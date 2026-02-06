package com.example.wq.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细实体
 */
@Entity
@Table(name = "order_item", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_product_id", columnList = "product_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单明细实体")
public class OrderItem extends AbstractHibernateBean {

    @Schema(description = "订单ID", example = "1703123456789_1234")
    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Schema(description = "商品ID", example = "1703123456789_5678")
    @Column(name = "product_id", length = 64, nullable = false)
    private String productId;

    @Schema(description = "商品名称", example = "钙加维D软胶囊")
    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Schema(description = "商品单价", example = "128.00")
    @Column(name = "product_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal productPrice;

    @Schema(description = "数量", example = "2")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Schema(description = "小计金额", example = "256.00")
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    // ========== 关联关系 ==========

    @Schema(description = "所属订单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
}

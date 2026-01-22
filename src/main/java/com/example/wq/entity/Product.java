package com.example.wq.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 产品实体
 */
@Entity
@Table(name = "product")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "产品实体")
public class Product extends AbstractHibernateBean {

    @Schema(description = "产品名称", example = "iPhone 15", required = true)
    @Column(name = "name", nullable = false)
    private String name;

    @Schema(description = "产品描述", example = "苹果最新款手机")
    @Column(name = "description")
    private String description;

    @Schema(description = "价格", example = "5999.99")
    @Column(name = "price")
    private BigDecimal price;

    @Schema(description = "分类", example = "电子产品")
    @Column(name = "category")
    private String category;

    @Schema(description = "库存", example = "100")
    @Column(name = "stock")
    private Integer stock = 0;

    @Schema(description = "状态", example = "1", allowableValues = {"1", "0"})
    @Column(name = "status")
    private Integer status = 1; // 1-上架, 0-下架
}


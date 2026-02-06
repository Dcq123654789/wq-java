package com.example.wq.entity;

import com.example.wq.enums.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品实体
 */
@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品实体")
public class Product extends AbstractHibernateBean {

    @Schema(description = "商品名称", example = "钙加维D软胶囊")
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Schema(description = "价格（元）", example = "128.00")
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Schema(description = "商品分类", example = "1", allowableValues = {"0", "1", "2", "3", "4"})
    @Column(name = "category", nullable = false)
    private Integer category;

    @Schema(description = "主图URL", example = "https://example.com/product.jpg")
    @Column(name = "poster", length = 500, nullable = false)
    private String poster;

    @Schema(description = "商品描述", example = "专为中老年人设计的钙补充剂...")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Schema(description = "库存数量", example = "100")
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Schema(description = "销量", example = "50")
    @Column(name = "sales", nullable = false)
    private Integer sales = 0;

    @Schema(description = "评分（0-5）", example = "4.5")
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Schema(description = "评价数量", example = "10")
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    // ========== 图片和规格 ==========

    @Schema(description = "商品图片数组（JSON数组）", example = "[\"https://example.com/img1.jpg\", \"https://example.com/img2.jpg\"]")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "json")
    private List<String> images;

    @Schema(description = "商品规格（如500g/100ml）", example = "500g")
    @Column(name = "spec", length = 50)
    private String spec;

    @Schema(description = "商品产地", example = "中国")
    @Column(name = "origin", length = 100)
    private String origin;

    @Schema(description = "保质期（月）", example = "24")
    @Column(name = "shelf_life", columnDefinition = "TINYINT UNSIGNED COMMENT '保质期（月）'")
    private Integer shelfLife; // 用数值型，方便后续筛选（如>12个月）

    // ========== 枚举转换方法 ==========

    /**
     * 获取商品分类枚举
     */
    @Transient
    public ProductCategory getCategoryEnum() {
        return ProductCategory.fromCode(this.category);
    }

    /**
     * 设置商品分类
     */
    public void setCategoryEnum(ProductCategory productCategory) {
        this.category = productCategory != null ? productCategory.getCode() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());
        }
        if (this.stock == null) {
            this.stock = 0;
        }
        if (this.sales == null) {
            this.sales = 0;
        }
        if (this.rating == null) {
            this.rating = BigDecimal.ZERO;
        }
        if (this.reviewCount == null) {
            this.reviewCount = 0;
        }
    }
}

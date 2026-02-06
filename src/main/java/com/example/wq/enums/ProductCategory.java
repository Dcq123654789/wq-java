package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 商品分类枚举
 */
public enum ProductCategory {

    /**
     * 营养保健
     */
    NUTRITION(0, "营养保健"),

    /**
     * 健康器械
     */
    DEVICE(1, "健康器械"),

    /**
     * 体检服务
     */
    CHECKUP(2, "体检服务"),

    /**
     * 健康咨询
     */
    CONSULTATION(3, "健康咨询"),

    /**
     * 其他
     */
    OTHER(4, "其他");

    private final Integer code;
    private final String description;

    ProductCategory(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ProductCategory fromCode(Integer code) {
        if (code == null) {
            return NUTRITION;
        }
        for (ProductCategory category : ProductCategory.values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return NUTRITION;
    }
}

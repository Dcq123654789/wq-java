package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 活动分类枚举
 */
public enum ActivityCategory {

    /**
     * 文化活动
     */
    CULTURE(0, "文化活动"),

    /**
     * 体育健身
     */
    SPORTS(1, "体育健身"),

    /**
     * 娱乐休闲
     */
    ENTERTAINMENT(2, "娱乐休闲"),

    /**
     * 志愿服务
     */
    VOLUNTEER(3, "志愿服务"),

    /**
     * 学习培训
     */
    LEARNING(4, "学习培训");

    private final Integer code;
    private final String description;

    ActivityCategory(Integer code, String description) {
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

    public static ActivityCategory fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ActivityCategory category : ActivityCategory.values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}

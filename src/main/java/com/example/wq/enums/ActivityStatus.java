package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 活动状态枚举
 */
public enum ActivityStatus {

    /**
     * 报名中
     */
    REGISTERING(0, "报名中"),

    /**
     * 报名结束
     */
    REGISTRATION_CLOSED(1, "报名结束"),

    /**
     * 活动结束
     */
    ACTIVITY_ENDED(2, "活动结束"),

    /**
     * 已满员
     */
    FULL(3, "已满员");

    private final Integer code;
    private final String description;

    ActivityStatus(Integer code, String description) {
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

    public static ActivityStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ActivityStatus status : ActivityStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

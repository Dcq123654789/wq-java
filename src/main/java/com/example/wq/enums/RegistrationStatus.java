package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 报名状态枚举
 */
public enum RegistrationStatus {

    /**
     * 已报名
     */
    REGISTERED(0, "已报名"),

    /**
     * 已取消
     */
    CANCELLED(1, "已取消"),

    /**
     * 已签到
     */
    CHECKED_IN(2, "已签到");

    private final Integer code;
    private final String description;

    RegistrationStatus(Integer code, String description) {
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

    public static RegistrationStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RegistrationStatus status : RegistrationStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

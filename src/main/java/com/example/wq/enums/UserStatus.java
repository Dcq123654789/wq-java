package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户状态枚举
 */
public enum UserStatus {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 正常
     */
    NORMAL(1, "正常");

    private final Integer code;
    private final String description;

    UserStatus(Integer code, String description) {
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

    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : UserStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

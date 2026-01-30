package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 后台管理员状态枚举
 */
public enum AdminUserStatus {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 锁定
     */
    LOCKED(2, "锁定");

    private final Integer code;
    private final String description;

    AdminUserStatus(Integer code, String description) {
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

    public static AdminUserStatus fromCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (AdminUserStatus status : AdminUserStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return NORMAL;
    }
}

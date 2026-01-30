package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 后台管理员角色枚举
 */
public enum AdminUserRole {

    /**
     * 超级管理员
     */
    SUPER_ADMIN(0, "超级管理员"),

    /**
     * 管理员
     */
    ADMIN(1, "管理员"),

    /**
     * 普通管理员
     */
    NORMAL_ADMIN(2, "普通管理员");

    private final Integer code;
    private final String description;

    AdminUserRole(Integer code, String description) {
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

    public static AdminUserRole fromCode(Integer code) {
        if (code == null) {
            return NORMAL_ADMIN;
        }
        for (AdminUserRole role : AdminUserRole.values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return NORMAL_ADMIN;
    }
}

package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 逻辑删除枚举
 */
public enum DeletedFlag {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    private final Integer code;
    private final String description;

    DeletedFlag(Integer code, String description) {
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

    public static DeletedFlag fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeletedFlag flag : DeletedFlag.values()) {
            if (flag.code.equals(code)) {
                return flag;
            }
        }
        return null;
    }
}

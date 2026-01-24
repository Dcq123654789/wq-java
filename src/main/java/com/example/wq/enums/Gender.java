package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 性别枚举
 */
public enum Gender {

    /**
     * 未知
     */
    UNKNOWN(0, "未知"),

    /**
     * 男
     */
    MALE(1, "男"),

    /**
     * 女
     */
    FEMALE(2, "女");

    private final Integer code;
    private final String description;

    Gender(Integer code, String description) {
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

    public static Gender fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (Gender gender : Gender.values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}

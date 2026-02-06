package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 地址标签枚举
 */
public enum AddressTag {

    /**
     * 家
     */
    HOME(0, "家"),

    /**
     * 公司
     */
    COMPANY(1, "公司"),

    /**
     * 学校
     */
    SCHOOL(2, "学校");

    private final Integer code;
    private final String description;

    AddressTag(Integer code, String description) {
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

    public static AddressTag fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AddressTag tag : AddressTag.values()) {
            if (tag.code.equals(code)) {
                return tag;
            }
        }
        return null;
    }
}

package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 是否枚举
 */
public enum YesNo {

    /**
     * 否
     */
    NO(0, "否"),

    /**
     * 是
     */
    YES(1, "是");

    private final Integer code;
    private final String description;

    YesNo(Integer code, String description) {
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

    public static YesNo fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (YesNo yesNo : YesNo.values()) {
            if (yesNo.code.equals(code)) {
                return yesNo;
            }
        }
        return null;
    }
}

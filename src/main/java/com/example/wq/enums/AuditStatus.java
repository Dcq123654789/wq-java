package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 审核状态枚举
 */
public enum AuditStatus {

    /**
     * 待审核
     */
    PENDING_AUDIT(0, "待审核"),

    /**
     * 审核通过
     */
    APPROVED(1, "审核通过"),

    /**
     * 审核拒绝
     */
    REJECTED(2, "审核拒绝");

    private final Integer code;
    private final String description;

    AuditStatus(Integer code, String description) {
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

    public static AuditStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AuditStatus status : AuditStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

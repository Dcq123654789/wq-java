package com.example.wq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {

    /**
     * 未支付
     */
    UNPAID(0, "未支付"),

    /**
     * 已支付
     */
    PAID(1, "已支付"),

    /**
     * 已退款
     */
    REFUNDED(2, "已退款");

    private final Integer code;
    private final String description;

    PaymentStatus(Integer code, String description) {
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

    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

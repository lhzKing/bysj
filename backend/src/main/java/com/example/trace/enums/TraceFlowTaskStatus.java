package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TraceFlowTaskStatus {

    CREATED("CREATED", "已创建"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    EXCEPTION("EXCEPTION", "异常");

    private final String code;
    private final String label;

    TraceFlowTaskStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean terminal() {
        return this == COMPLETED || this == CANCELLED || this == EXCEPTION;
    }

    @JsonCreator
    public static TraceFlowTaskStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceFlowTaskStatus 不能为空");
        }
        String normalized = value.trim().toUpperCase();
        for (TraceFlowTaskStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("非法的 TraceFlowTaskStatus: " + value);
    }
}

package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TraceFlowTaskType {

    OUTBOUND("OUTBOUND", "出库任务"),
    TRANSFER("TRANSFER", "运输/流转任务"),
    INBOUND("INBOUND", "入库任务"),
    RECEIVE("RECEIVE", "接收确认任务");

    private final String code;
    private final String label;

    TraceFlowTaskType(String code, String label) {
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

    @JsonCreator
    public static TraceFlowTaskType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceFlowTaskType 不能为空");
        }
        String normalized = value.trim().toUpperCase();
        for (TraceFlowTaskType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("非法的 TraceFlowTaskType: " + value);
    }
}

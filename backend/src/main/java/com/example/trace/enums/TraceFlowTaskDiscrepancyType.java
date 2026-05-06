package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TraceFlowTaskDiscrepancyType {

    NONE("NONE", "无差异"),
    SHORTAGE("SHORTAGE", "少扫"),
    OVERAGE("OVERAGE", "多扫");

    private final String code;
    private final String label;

    TraceFlowTaskDiscrepancyType(String code, String label) {
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
    public static TraceFlowTaskDiscrepancyType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceFlowTaskDiscrepancyType 不能为空");
        }
        String normalized = value.trim().toUpperCase();
        for (TraceFlowTaskDiscrepancyType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("非法的 TraceFlowTaskDiscrepancyType: " + value);
    }
}

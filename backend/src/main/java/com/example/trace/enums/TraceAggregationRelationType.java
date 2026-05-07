package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Parent-child aggregation relation type.
 * CARTON means a carton parent contains single-item trace codes; PALLET means
 * a pallet parent contains cartons or directly contains single-item trace codes.
 */
public enum TraceAggregationRelationType {

    CARTON("CARTON", "箱码"),
    PALLET("PALLET", "托盘码"),
    BATCH("BATCH", "批量聚合");

    private final String code;
    private final String label;

    TraceAggregationRelationType(String code, String label) {
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
    public static TraceAggregationRelationType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceAggregationRelationType 不能为空");
        }
        String normalized = value.trim().toUpperCase();
        for (TraceAggregationRelationType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("非法的 TraceAggregationRelationType: " + value);
    }
}

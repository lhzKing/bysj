package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Structured node type for trace logistics/business locations.
 */
public enum TraceNodeType {

    FACTORY,
    WAREHOUSE,
    LOGISTICS,
    CUSTOMER,
    SERVICE;

    @JsonValue
    public String getCode() {
        return name();
    }

    @JsonCreator
    public static TraceNodeType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceNodeType must not be blank");
        }
        String normalized = value.trim().toUpperCase();
        for (TraceNodeType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Illegal TraceNodeType: " + value);
    }
}

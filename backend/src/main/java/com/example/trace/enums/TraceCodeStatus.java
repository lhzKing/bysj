package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Single-item trace-code status.
 *
 * <p>{@link TraceStatus} describes the latest logistics lifecycle snapshot
 * (where the item is now). This enum describes whether the physical label/code
 * itself is usable: generated/printed labels are not yet bound to a real item,
 * activated labels can enter lifecycle flow, and terminal labels cannot flow.</p>
 */
public enum TraceCodeStatus {

    /** Code has been generated but the label has not been printed yet. */
    GENERATED,

    /** Label has been printed, but it has not been attached and verified. */
    PRINTED,

    /** Label has been attached to a real item and scan-verified. */
    ACTIVATED,

    /** Activated item is currently in stock. */
    IN_STOCK,

    /** Activated item is currently in transit. */
    IN_TRANSIT,

    /** Code/item is in an exception-hold state. */
    EXCEPTION,

    /** Unactivated code was voided and must never be reused. */
    VOIDED,

    /** Physical item was scrapped and the code is terminal. */
    SCRAPPED;

    @JsonValue
    public String getCode() {
        return name();
    }

    @JsonCreator
    public static TraceCodeStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceCodeStatus must not be blank");
        }

        String normalized = value.trim().toUpperCase();
        for (TraceCodeStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Illegal TraceCodeStatus: " + value);
    }

    /**
     * Whether a code in this status may enter normal lifecycle movements
     * (inbound, outbound, transfer). GENERATED/PRINTED are intentionally
     * blocked until B12 activation; VOIDED/SCRAPPED/EXCEPTION are terminal or
     * frozen for normal movements.
     */
    public boolean allowsLifecycleMovement() {
        return this == ACTIVATED || this == IN_STOCK || this == IN_TRANSIT;
    }

    public boolean isTerminal() {
        return this == VOIDED || this == SCRAPPED;
    }

    /**
     * Maps the current snapshot lifecycle status into the coarser code-status
     * mirror after a successful lifecycle event.
     */
    public static TraceCodeStatus fromTraceStatus(TraceStatus traceStatus) {
        if (traceStatus == null) {
            return ACTIVATED;
        }

        return switch (traceStatus) {
            case INIT -> ACTIVATED;
            case IN_STOCK -> IN_STOCK;
            case IN_TRANSIT, TRANSFERRED -> IN_TRANSIT;
            case EXCEPTION -> EXCEPTION;
        };
    }
}

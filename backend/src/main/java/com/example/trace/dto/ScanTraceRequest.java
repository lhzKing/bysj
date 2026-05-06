package com.example.trace.dto;

import com.example.trace.enums.ActionType;
import com.example.trace.validation.TraceLocationFieldConstraints;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScanTraceRequest {

    @JsonIgnore
    private String traceCode;

    @JsonIgnore
    private Long operatorUserId;

    @NotNull(message = "actionType must not be null")
    private ActionType actionType;

    @Size(max = TraceLocationFieldConstraints.NODE_MAX_LENGTH, message = "fromNode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "fromNode contains unsupported characters")
    private String fromNode;

    @Size(max = TraceLocationFieldConstraints.NODE_MAX_LENGTH, message = "toNode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "toNode contains unsupported characters")
    private String toNode;

    @Size(max = TraceLocationFieldConstraints.REGION_MAX_LENGTH, message = "province length must be <= 32")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "province contains unsupported characters")
    private String province;

    @Size(max = TraceLocationFieldConstraints.REGION_MAX_LENGTH, message = "city length must be <= 32")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "city contains unsupported characters")
    private String city;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;

    private String eventTime;
    private Long correctionOf;

    @Size(max = TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH,
            message = "idempotencyKey length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "idempotencyKey contains unsupported characters")
    private String idempotencyKey;
}

package com.example.trace.dto;

import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceCodeActivateRequest {

    private String eventTime;

    @Size(max = TraceLocationFieldConstraints.NODE_MAX_LENGTH, message = "activationNode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "activationNode contains unsupported characters")
    private String activationNode;

    @Size(max = TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH, message = "deviceId length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "deviceId contains unsupported characters")
    private String deviceId;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;
}

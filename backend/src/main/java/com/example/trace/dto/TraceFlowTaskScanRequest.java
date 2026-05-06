package com.example.trace.dto;

import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceFlowTaskScanRequest {

    @NotBlank(message = "traceCode must not be blank")
    @Size(max = TraceLocationFieldConstraints.NODE_MAX_LENGTH,
            message = "traceCode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.SAFE_TEXT_PATTERN,
            message = "traceCode contains unsupported characters")
    private String traceCode;

    private String eventTime;

    @Size(max = TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH,
            message = "idempotencyKey length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "idempotencyKey contains unsupported characters")
    private String idempotencyKey;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH,
            message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;
}

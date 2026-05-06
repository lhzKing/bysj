package com.example.trace.dto;

import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceFlowTaskCompleteRequest {

    @Min(value = 0, message = "actualQuantity must be >= 0")
    @Max(value = 100000, message = "actualQuantity must be <= 100000")
    private Integer actualQuantity;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH,
            message = "discrepancyReason length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "discrepancyReason contains unsupported characters")
    private String discrepancyReason;
}

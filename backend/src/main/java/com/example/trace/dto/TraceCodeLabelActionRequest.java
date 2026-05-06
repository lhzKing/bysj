package com.example.trace.dto;

import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceCodeLabelActionRequest {

    private String eventTime;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;
}

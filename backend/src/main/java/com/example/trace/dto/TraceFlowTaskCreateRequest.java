package com.example.trace.dto;

import com.example.trace.enums.TraceFlowTaskType;
import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceFlowTaskCreateRequest {

    @Size(max = 64, message = "taskNo length must be <= 64")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "taskNo contains unsupported characters")
    private String taskNo;

    @NotNull(message = "taskType must not be null")
    private TraceFlowTaskType taskType;

    @NotNull(message = "sourceNodeId must not be null")
    private Long sourceNodeId;

    @NotNull(message = "targetNodeId must not be null")
    private Long targetNodeId;

    @NotNull(message = "expectedQuantity must not be null")
    @Min(value = 1, message = "expectedQuantity must be >= 1")
    @Max(value = 100000, message = "expectedQuantity must be <= 100000")
    private Integer expectedQuantity;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;
}

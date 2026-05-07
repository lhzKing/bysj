package com.example.trace.dto;

import com.example.trace.enums.TraceAggregationRelationType;
import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceAggregationBindRequest {

    @NotBlank(message = "parentCode must not be blank")
    @Size(max = TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH,
            message = "parentCode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "parentCode contains unsupported characters")
    private String parentCode;

    @NotBlank(message = "childCode must not be blank")
    @Size(max = TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH,
            message = "childCode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "childCode contains unsupported characters")
    private String childCode;

    @NotNull(message = "relationType must not be null")
    private TraceAggregationRelationType relationType;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;
}

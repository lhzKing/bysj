package com.example.trace.dto;

import com.example.trace.enums.TraceNodeType;
import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraceNodeCreateRequest {

    @NotBlank(message = "nodeCode must not be blank")
    @Size(max = 64, message = "nodeCode length must be <= 64")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "nodeCode contains unsupported characters")
    private String nodeCode;

    @NotBlank(message = "nodeName must not be blank")
    @Size(max = TraceLocationFieldConstraints.NODE_MAX_LENGTH, message = "nodeName length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "nodeName contains unsupported characters")
    private String nodeName;

    @NotNull(message = "nodeType must not be null")
    private TraceNodeType nodeType;

    private Long orgId;

    @NotBlank(message = "province must not be blank")
    @Size(max = TraceLocationFieldConstraints.REGION_MAX_LENGTH, message = "province length must be <= 32")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "province contains unsupported characters")
    private String province;

    @NotBlank(message = "city must not be blank")
    @Size(max = TraceLocationFieldConstraints.REGION_MAX_LENGTH, message = "city length must be <= 32")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "city contains unsupported characters")
    private String city;

    @Size(max = 255, message = "address length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "address contains unsupported characters")
    private String address;

    private Boolean enabled = true;
}

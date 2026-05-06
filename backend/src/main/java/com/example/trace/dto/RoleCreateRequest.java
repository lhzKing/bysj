package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {

    @NotBlank(message = "roleCode must not be blank")
    @Size(min = 2, max = 50, message = "roleCode length must be between 2 and 50")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "roleCode must start with an uppercase letter and contain only uppercase letters, digits, or underscores")
    @JsonAlias("roleCode")
    private String roleCode;

    @NotBlank(message = "roleName must not be blank")
    @Size(min = 2, max = 100, message = "roleName length must be between 2 and 100")
    @JsonAlias("roleName")
    private String roleName;

    @Size(max = 500, message = "remark length must be at most 500")
    private String remark;
}
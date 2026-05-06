package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @Size(min = 2, max = 100, message = "roleName length must be between 2 and 100")
    @JsonAlias("roleName")
    private String roleName;

    @Size(max = 500, message = "remark length must be at most 500")
    private String remark;
}
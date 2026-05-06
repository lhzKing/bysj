package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsRequest {

    @NotEmpty(message = "permissionIds must not be empty")
    @JsonAlias("permissionIds")
    private List<Long> permissionIds;
}
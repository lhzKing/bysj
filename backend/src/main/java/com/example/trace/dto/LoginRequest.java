package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @JsonAlias("rememberMe")
    private Boolean rememberMe;

    public boolean isRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
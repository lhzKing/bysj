package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 50, message = "username length must be between 3 and 50")
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 100, message = "password length must be between 6 and 100")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "password must contain letters and digits")
    private String password;

    @NotNull(message = "roleId must not be null")
    @JsonAlias("roleId")
    private Long roleId;

    @Min(value = 0, message = "status must be 0 or 1")
    @Max(value = 1, message = "status must be 0 or 1")
    private Integer status = 1;
}

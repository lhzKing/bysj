package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 6, max = 100, message = "password length must be between 6 and 100")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "password must contain letters and digits")
    @JsonAlias("newPassword")
    private String newPassword;
}
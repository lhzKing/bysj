package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PartCreateRequest {

    @NotBlank(message = "partCode must not be blank")
    @Size(min = 2, max = 50, message = "partCode length must be between 2 and 50")
    @JsonAlias("partCode")
    private String partCode;

    @NotBlank(message = "partName must not be blank")
    @Size(min = 2, max = 200, message = "partName length must be between 2 and 200")
    @JsonAlias("partName")
    private String partName;

    @NotBlank(message = "partType must not be blank")
    @Size(max = 100, message = "partType length must be at most 100")
    @JsonAlias("partType")
    private String partType;

    @Size(max = 200, message = "model length must be at most 200")
    private String model;

    @Size(max = 200, message = "manufacturer length must be at most 200")
    private String manufacturer;

    @Size(max = 20, message = "unit length must be at most 20")
    private String unit;

    @Size(max = 500, message = "remark length must be at most 500")
    private String remark;
}
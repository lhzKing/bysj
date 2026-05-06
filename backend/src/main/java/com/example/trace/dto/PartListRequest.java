package com.example.trace.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PartListRequest extends PageRequest {

    private String keyword;
    private String partCode;
    private String partName;
    private String partType;
    private String manufacturer;
}

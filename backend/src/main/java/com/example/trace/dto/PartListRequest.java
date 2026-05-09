package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /**
     * 启停过滤：true=只看启用 / false=只看禁用 / null=不过滤。
     */
    @JsonAlias("enabled")
    private Boolean enabled;
}

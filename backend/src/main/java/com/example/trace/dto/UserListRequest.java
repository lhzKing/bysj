package com.example.trace.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserListRequest extends PageRequest {

    private String username;
    private Long roleId;
    private Integer status;
}

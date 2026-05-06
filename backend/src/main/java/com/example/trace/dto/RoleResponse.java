package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应
 */
@Data
public class RoleResponse {
    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 权限数量（列表接口轻量返回）
     */
    private Integer permissionCount;

    /**
     * 权限列表（仅在详情接口返回）
     */
    private List<PermissionResponse> permissions;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

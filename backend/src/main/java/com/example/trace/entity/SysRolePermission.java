package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 角色权限关联实体
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 权限ID
     */
    private Long permissionId;
}

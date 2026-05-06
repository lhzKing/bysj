package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色代码，如 ADMIN, PRODUCER
     */
    private String roleCode;

    /**
     * 角色名称，如 "系统管理员"
     */
    private String roleName;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createTime;
}

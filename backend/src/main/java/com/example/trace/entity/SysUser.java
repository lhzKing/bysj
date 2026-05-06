package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;

    /**
     * 角色ID（关联 sys_role 表）
     */
    private Long roleId;

    /**
     * 角色信息（非数据库字段，关联查询时填充）
     */
    @TableField(exist = false)
    private SysRole role;

    /**
     * 角色代码（非数据库字段，方便使用）
     */
    @TableField(exist = false)
    private String roleCode;

    /**
     * Token 版本号（用于强制失效所有 Token）
     * 每次修改密码或角色变更时 +1
     */
    private Integer tokenVersion;

    /**
     * 状态：1=正常，0=禁用
     */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 判断用户是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 获取 Token 版本号（默认0）
     */
    public Integer getTokenVersion() {
        return tokenVersion != null ? tokenVersion : 0;
    }
}

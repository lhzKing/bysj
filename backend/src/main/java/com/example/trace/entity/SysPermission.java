package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限代码，如 trace:create
     */
    private String permCode;

    /**
     * 权限名称，如 "生产赋码"
     */
    private String permName;

    /**
     * HTTP 方法：GET/POST/PUT/DELETE/*
     */
    private String apiMethod;

    /**
     * API 路径模式，支持 * 通配符
     */
    private String apiPattern;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createTime;
}

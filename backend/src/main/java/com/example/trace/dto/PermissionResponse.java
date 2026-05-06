package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限响应
 */
@Data
public class PermissionResponse {
    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限编码
     */
    private String permCode;

    /**
     * 权限名称
     */
    private String permName;

    /**
     * API方法（GET/POST/PUT/DELETE等）
     */
    private String apiMethod;

    /**
     * API路径模式
     */
    private String apiPattern;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

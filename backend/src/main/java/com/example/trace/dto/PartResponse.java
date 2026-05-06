package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配件响应
 */
@Data
public class PartResponse {
    /**
     * 配件ID
     */
    private Long id;

    /**
     * 配件编码
     */
    private String partCode;

    /**
     * 配件名称
     */
    private String partName;

    /**
     * 配件类型
     */
    private String partType;

    /**
     * 型号规格
     */
    private String model;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

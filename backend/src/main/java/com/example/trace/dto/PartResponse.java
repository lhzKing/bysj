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
     * 启停标志：true=启用（默认）/ false=禁用。禁用后仍保留历史溯源数据，
     * 但生产赋码等写入流程应拒绝使用该 SPU。
     */
    private Boolean enabled;

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

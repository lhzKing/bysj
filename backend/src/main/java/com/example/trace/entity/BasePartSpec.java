package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("base_part_spec")
public class BasePartSpec {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partCode;
    private String partName;
    private String partType;
    private String model;
    private String manufacturer;
    private String unit;
    private String remark;

    /**
     * 启停标志：true=启用（默认）/ false=禁用。禁用后仍保留历史溯源数据，
     * 但生产赋码等写入流程应拒绝使用该 SPU。
     */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

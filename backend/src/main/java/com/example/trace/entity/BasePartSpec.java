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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

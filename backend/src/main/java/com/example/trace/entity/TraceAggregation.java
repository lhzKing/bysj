package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_aggregation")
public class TraceAggregation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String parentCode;
    private String childCode;
    private String relationType;
    private Boolean active;
    private Long createBy;
    private String createByUsername;
    private LocalDateTime bindTime;
    private LocalDateTime releaseTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_flow_task")
public class TraceFlowTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private String taskType;
    private Long sourceNodeId;
    private Long targetNodeId;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private String status;
    private Long createBy;
    private String createByUsername;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private String discrepancyType;
    private Integer discrepancyQuantity;
    private String discrepancyReason;
    private LocalDateTime discrepancyTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_flow_task_scan")
public class TraceFlowTaskScan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private String traceCode;
    private String actionType;
    private Boolean counted;
    private Long operatorUserId;
    private String operatorUsername;
    private String idempotencyKey;
    private LocalDateTime scanTime;
    private Integer duplicateCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

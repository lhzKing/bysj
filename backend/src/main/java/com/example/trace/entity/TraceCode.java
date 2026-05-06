package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_code")
public class TraceCode {

    @TableId
    private String traceCode;

    private Long batchId;
    private Long spuId;
    private Integer serialNo;

    private String qrPayload;
    private String codeStatus;
    private Integer printCount;

    private LocalDateTime activatedTime;
    private Long activatedBy;
    private String activatedByUsername;

    /**
     * Current snapshot pointer. It stores trace_snapshot.trace_code for the
     * current schema because trace_snapshot uses trace_code as its primary key.
     */
    private String currentSnapshotId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

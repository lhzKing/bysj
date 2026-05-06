package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_scan_idempotency")
public class TraceScanIdempotency {

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceCode;
    private String actionType;
    private String idempotencyKey;
    private Long lifecycleLogId;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public boolean isSucceeded() {
        return STATUS_SUCCEEDED.equals(status) && lifecycleLogId != null;
    }
}

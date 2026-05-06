package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_snapshot")
public class TraceSnapshot {

    @TableId
    private String traceCode;

    private Long spuId;

    private String currentStatus;
    private String currentNode;
    private String currentOwner;

    private String province;
    private String city;

    private LocalDateTime lastEventTime;
    private Long lastLogId;
    private String lastHash;

    /**
     * 乐观锁版本号
     * 用于并发控制，替代悲观锁 FOR UPDATE
     */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

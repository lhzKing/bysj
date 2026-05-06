package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_assign_batch")
public class TraceAssignBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;
    private String productionOrderNo;
    private Long spuId;

    private Integer quantityRequested;
    private Integer quantityGenerated;
    private Integer quantityPrinted;
    private Integer quantityActivated;

    /**
     * Future structured node id. Nullable until B14 introduces trace_node.
     */
    private Long manufacturerNodeId;

    private String status;

    private Long operatorId;
    private String operatorUsername;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

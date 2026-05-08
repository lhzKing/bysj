package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 溯源码列表行响应。
 *
 * <p>聚合了 trace_snapshot + base_part_spec + trace_code + trace_assign_batch
 * 关键字段，避免前端为每条码再发一次详情请求；不返回 hash / signature 字段，
 * 链验证仍走 GET /api/traces/{code}/verify。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceListItemResponse {

    private String traceCode;

    private Long spuId;
    private String spuPartCode;
    private String spuPartName;
    private String spuPartType;

    private String currentStatus;
    private String currentNode;
    private String currentOwner;
    private String province;
    private String city;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastEventTime;

    private Long lastLogId;

    /**
     * 最近一次写入的 action_type，可能是 INIT/INBOUND/OUTBOUND/CORRECTION 等。
     */
    private String lastActionType;

    /**
     * 该追溯码的批次（来自 trace_assign_batch）；按需返回。
     */
    private Long batchId;
    private String batchNo;

    /**
     * 单品码状态（trace_code.code_status），可能 GENERATED/PRINTED/ACTIVATED/IN_STOCK/...
     */
    private String codeStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

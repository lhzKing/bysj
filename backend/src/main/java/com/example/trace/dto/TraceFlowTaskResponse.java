package com.example.trace.dto;

import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceFlowTaskDiscrepancyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceFlowTaskResponse {

    private Long id;
    private String taskNo;
    private TraceFlowTaskType taskType;
    private String taskTypeLabel;
    private TraceFlowTaskStatus status;
    private String statusLabel;

    private Long sourceNodeId;
    private String sourceNodeCode;
    private String sourceNodeName;
    private Long targetNodeId;
    private String targetNodeCode;
    private String targetNodeName;

    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer remainingQuantity;
    private TraceFlowTaskDiscrepancyType discrepancyType;
    private String discrepancyTypeLabel;
    private Integer discrepancyQuantity;
    private String discrepancyReason;
    private Long createBy;
    private String createByUsername;
    private String remark;

    /**
     * Scan-only feedback fields. They are populated by
     * POST /api/trace-flow-tasks/{id}/scan so continuous-scan clients can show
     * "accepted" vs "already scanned" without changing the stable task shape.
     */
    private String lastScanTraceCode;
    private ActionType lastScanActionType;
    private String lastScanActionLabel;
    private Boolean lastScanCreated;
    private Boolean duplicateScan;
    private String scanMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime discrepancyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
